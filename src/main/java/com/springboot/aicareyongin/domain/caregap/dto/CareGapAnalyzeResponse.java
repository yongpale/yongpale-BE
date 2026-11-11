package com.springboot.aicareyongin.domain.caregap.dto;

import com.springboot.aicareyongin.domain.caregap.client.AiCareGapResponse.TimeRange;
import com.springboot.aicareyongin.domain.caregap.enums.CarePeriod;
import com.springboot.aicareyongin.domain.household.enums.ChildGrade;
import com.springboot.aicareyongin.domain.policy.enums.MatchStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 돌봄 공백 분석 결과.
 *
 * @param child          분석한 자녀
 * @param children       가구의 자녀 목록. selectable=true인 자녀의 childId로 다시 요청하면 그 자녀로 분석한다.
 * @param gaps           돌봄 공백 시간대
 * @param plans          공백을 메우는 시설 조합 (남는 공백이 적은 순)
 * @param relatedPolicies 시설로 못 메우는 공백에 이용할 수 있는 돌봄 정책
 */
public record CareGapAnalyzeResponse(
        ChildOption child,
        List<ChildOption> children,
        CarePeriod period,
        TimeRange careTime,
        String schoolEndTime,
        List<TimeRange> gaps,
        int gapMinutes,
        List<Plan> plans,
        List<RelatedPolicy> relatedPolicies,
        List<String> notes) {

    /** selectable: 초등학생이라 돌봄 공백을 분석할 수 있는 자녀 */
    public record ChildOption(Long childId, String nickname, ChildGrade grade, boolean selectable) {
    }

    public record Plan(
            List<PlanFacility> facilities,
            int coveredMinutes,
            double coverageRate,
            List<TimeRange> remaining,
            List<String> reasons) {
    }

    /**
     * @param facilityId 백엔드 시설 ID (시설 상세 이동). 백엔드에 없는 시설이면 null
     * @param hours      해당 기간 운영시간
     * @param covers     이 시설이 맡는 시간
     */
    public record PlanFacility(
            Long facilityId,
            String name,
            String facilityType,
            String guName,
            String dongName,
            String address,
            String phone,
            BigDecimal latitude,
            BigDecimal longitude,
            TimeRange hours,
            TimeRange covers,
            Map<String, Object> fee,
            Boolean meal,
            Boolean snack,
            Boolean pickup) {
    }

    /** policyId: 백엔드 정책 ID (정책 상세 이동). 백엔드에 없는 정책이면 null */
    public record RelatedPolicy(Long policyId, String title, MatchStatus status, String reason) {
    }
}
