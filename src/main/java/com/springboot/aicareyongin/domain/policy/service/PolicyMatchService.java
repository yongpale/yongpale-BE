package com.springboot.aicareyongin.domain.policy.service;

import com.springboot.aicareyongin.domain.policy.client.AiMatchRequest;
import com.springboot.aicareyongin.domain.policy.client.AiMatchResponse;
import com.springboot.aicareyongin.domain.policy.client.AiMatchResponse.AiPolicyMatch;
import com.springboot.aicareyongin.domain.policy.client.MatchProfile;
import com.springboot.aicareyongin.domain.policy.client.MatchProfileReader;
import com.springboot.aicareyongin.domain.policy.dto.PolicyMatchListResponse;
import com.springboot.aicareyongin.domain.policy.dto.PolicyMatchResponse;
import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import com.springboot.aicareyongin.domain.policy.repository.PolicyRepository;
import com.springboot.aicareyongin.domain.policy.repository.PolicySpecs;
import com.springboot.aicareyongin.global.ai.AiServerClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 맞춤 정책 매칭. 판정은 AI 서버(규칙 기반)가 하고, 백엔드는 가구 정보를 넘긴 뒤
 * 결과의 source.url로 백엔드 정책을 찾아 policyId를 붙인다.
 * AI 호출 중 DB 커넥션을 잡지 않도록 클래스 단위 트랜잭션을 두지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyMatchService {

    private static final String MATCH_PATH = "/ai/policies/match";

    private final MatchProfileReader matchProfileReader;
    private final PolicyRepository policyRepository;
    private final AiServerClient aiServerClient;

    public PolicyMatchListResponse getMatches(Long userId, PolicyCategory category) {
        Map<String, Policy> policies = activePoliciesBySourceUrl();
        List<PolicyMatchResponse> matches = new ArrayList<>();
        for (AiPolicyMatch match : requestMatches(userId, false).policies()) {
            Policy policy = policies.get(sourceUrl(match));
            if (policy == null) {
                // AI 서버와 백엔드의 정책 데이터가 어긋난 경우. 화면에서 상세로 이동할 수 없으므로 뺀다.
                log.warn("백엔드에 없는 정책이 매칭 결과에 있습니다. aiPolicyId={} name={}", match.id(), match.name());
                continue;
            }
            if (category == null || policy.getCategory() == category) {
                matches.add(PolicyMatchResponse.of(policy, match));
            }
        }
        return PolicyMatchListResponse.of(matches);
    }

    public PolicyMatchResponse getMatch(Long userId, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .filter(Policy::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 정책입니다."));
        // 해당 안 됨인 정책도 이유를 보여줘야 하므로 ineligible까지 받는다.
        return requestMatches(userId, true).policies().stream()
                .filter(match -> policy.getSourceUrl().equals(sourceUrl(match)))
                .findFirst()
                .map(match -> PolicyMatchResponse.of(policy, match))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이 정책의 매칭 정보가 없습니다."));
    }

    private AiMatchResponse requestMatches(Long userId, boolean includeIneligible) {
        // 가구 정보가 없으면 404(등록된 가구 정보가 없습니다)
        MatchProfile profile = matchProfileReader.read(userId);
        AiMatchResponse response = aiServerClient.post(MATCH_PATH,
                new AiMatchRequest(profile, includeIneligible), AiMatchResponse.class);
        if (response == null || response.policies() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버 응답이 올바르지 않습니다.");
        }
        return response;
    }

    private Map<String, Policy> activePoliciesBySourceUrl() {
        return policyRepository.findAll(PolicySpecs.active()).stream()
                .collect(Collectors.toMap(Policy::getSourceUrl, Function.identity(), (a, b) -> a));
    }

    private String sourceUrl(AiPolicyMatch match) {
        return match.source() == null ? null : match.source().url();
    }
}
