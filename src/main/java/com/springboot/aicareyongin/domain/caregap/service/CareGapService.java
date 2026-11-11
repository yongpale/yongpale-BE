package com.springboot.aicareyongin.domain.caregap.service;

import com.springboot.aicareyongin.domain.caregap.client.AiCareGapRequest;
import com.springboot.aicareyongin.domain.caregap.client.AiCareGapResponse;
import com.springboot.aicareyongin.domain.caregap.dto.CareGapAnalyzeRequest;
import com.springboot.aicareyongin.domain.caregap.dto.CareGapAnalyzeResponse;
import com.springboot.aicareyongin.domain.caregap.dto.CareGapAnalyzeResponse.ChildOption;
import com.springboot.aicareyongin.domain.caregap.enums.CarePeriod;
import com.springboot.aicareyongin.domain.facility.entity.Facility;
import com.springboot.aicareyongin.domain.facility.repository.FacilityRepository;
import com.springboot.aicareyongin.domain.policy.client.AiHousehold;
import com.springboot.aicareyongin.domain.policy.client.AiHousehold.ChildInfo;
import com.springboot.aicareyongin.domain.policy.client.MatchProfile;
import com.springboot.aicareyongin.domain.policy.client.MatchProfileReader;
import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.MatchStatus;
import com.springboot.aicareyongin.domain.policy.repository.PolicyRepository;
import com.springboot.aicareyongin.domain.policy.repository.PolicySpecs;
import com.springboot.aicareyongin.global.ai.AiServerClient;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 하교 후 돌봄 공백 분석. 계산은 AI 서버(규칙 기반)가 하고, 백엔드는 자녀를 고르고 가구 정보를 넘긴 뒤
 * 결과의 시설·정책에 백엔드 ID를 붙인다. AI 호출 중 DB 커넥션을 잡지 않도록 트랜잭션을 두지 않는다.
 */
@Service
@RequiredArgsConstructor
public class CareGapService {

    private static final String CARE_GAP_PATH = "/ai/care-gap";
    private static final int MAX_PLANS = 3;
    private static final Pattern SVC_ID = Pattern.compile("svcId=([^&#]+)");

    private final MatchProfileReader matchProfileReader;
    private final FacilityRepository facilityRepository;
    private final PolicyRepository policyRepository;
    private final AiServerClient aiServerClient;

    public CareGapAnalyzeResponse analyze(Long userId, CareGapAnalyzeRequest request) {
        AiHousehold household = matchProfileReader.readHousehold(userId);
        int childIndex = selectChild(household.children(), request.childId());
        CarePeriod period = request.periodOrDefault();

        AiCareGapRequest aiRequest = new AiCareGapRequest(
                household.profile(),
                childIndex,
                period.aiValue(),
                period == CarePeriod.SEMESTER ? request.schoolEndTime() : null,
                request.hasCareNeedTime()
                        ? new MatchProfile.CareTime(request.careNeedStartTime(), request.careNeedEndTime())
                        : null,
                MAX_PLANS);
        AiCareGapResponse ai = aiServerClient.post(CARE_GAP_PATH, aiRequest, AiCareGapResponse.class);
        if (ai == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버 응답이 올바르지 않습니다.");
        }

        List<ChildOption> children = household.children().stream().map(this::toOption).toList();
        return new CareGapAnalyzeResponse(
                children.get(childIndex),
                children,
                period,
                ai.careTime(),
                ai.schoolEnd(),
                nullToEmpty(ai.gap()),
                ai.gapMinutes(),
                toPlans(nullToEmpty(ai.plans())),
                toPolicies(nullToEmpty(ai.relatedPolicies())),
                nullToEmpty(ai.notes()));
    }

    /**
     * 분석할 자녀의 순서(0부터)를 고른다. childId가 없으면 학년이 가장 낮은 초등학생 자녀
     * (하교가 가장 이르고 돌봄이 가장 많이 필요).
     */
    private int selectChild(List<ChildInfo> children, Long childId) {
        if (childId != null) {
            int index = IntStream.range(0, children.size())
                    .filter(i -> children.get(i).childId().equals(childId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "가구에 등록된 자녀가 아닙니다."));
            if (!children.get(index).isElementary()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "돌봄 공백 분석은 초등학생 자녀만 할 수 있습니다.");
            }
            return index;
        }
        return IntStream.range(0, children.size())
                .filter(i -> children.get(i).isElementary())
                .boxed()
                .min(Comparator.comparingInt(i -> children.get(i).grade().getGradeOrder()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "돌봄 공백 분석은 초등학생 자녀만 할 수 있습니다. 초등학생 자녀가 없습니다."));
    }

    private ChildOption toOption(ChildInfo child) {
        return new ChildOption(child.childId(), child.nickname(), child.grade(), child.isElementary());
    }

    private List<CareGapAnalyzeResponse.Plan> toPlans(List<AiCareGapResponse.Plan> plans) {
        Map<String, Facility> facilities = facilityRepository.findAll().stream()
                .filter(f -> f.getSourceRefId() != null)
                .collect(Collectors.toMap(Facility::getSourceRefId, Function.identity(), (a, b) -> a));
        return plans.stream()
                .map(plan -> new CareGapAnalyzeResponse.Plan(
                        nullToEmpty(plan.facilities()).stream().map(f -> toFacility(f, facilities.get(f.id()))).toList(),
                        plan.coveredMinutes(),
                        plan.coverageRate(),
                        nullToEmpty(plan.remaining()),
                        nullToEmpty(plan.reasons())))
                .toList();
    }

    // 이름·주소·좌표는 백엔드 시설 데이터를 쓰고, 운영시간·이용료처럼 분석에 쓴 값은 AI 결과를 쓴다.
    private CareGapAnalyzeResponse.PlanFacility toFacility(AiCareGapResponse.Facility ai, Facility facility) {
        if (facility == null) {
            return new CareGapAnalyzeResponse.PlanFacility(null, ai.name(), null, null, null, null, null, null, null,
                    ai.hours(), ai.covers(), ai.fee(), ai.meal(), ai.snack(), ai.pickup());
        }
        return new CareGapAnalyzeResponse.PlanFacility(
                facility.getId(), facility.getName(), facility.getSubtype(), facility.getGuName(),
                facility.getDongName(), facility.getAddress(), facility.getPhone(),
                facility.getLatitude(), facility.getLongitude(),
                ai.hours(), ai.covers(), ai.fee(), ai.meal(), ai.snack(), ai.pickup());
    }

    private List<CareGapAnalyzeResponse.RelatedPolicy> toPolicies(List<AiCareGapResponse.RelatedPolicy> related) {
        Map<String, Policy> bySvcId = policyRepository.findAll(PolicySpecs.active()).stream()
                .filter(p -> svcId(p.getSourceUrl()) != null)
                .collect(Collectors.toMap(p -> svcId(p.getSourceUrl()), Function.identity(), (a, b) -> a));
        return related.stream()
                .map(r -> {
                    Policy policy = bySvcId.get(r.id());
                    return new CareGapAnalyzeResponse.RelatedPolicy(
                            policy == null ? null : policy.getId(),
                            policy == null ? r.name() : policy.getTitle(),
                            r.status() == null ? null : MatchStatus.fromAi(r.status()),
                            r.reason());
                })
                .toList();
    }

    private String svcId(String sourceUrl) {
        if (sourceUrl == null) {
            return null;
        }
        Matcher m = SVC_ID.matcher(sourceUrl);
        return m.find() ? m.group(1) : null;
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
