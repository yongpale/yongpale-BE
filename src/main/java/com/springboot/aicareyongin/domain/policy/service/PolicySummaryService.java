package com.springboot.aicareyongin.domain.policy.service;

import com.springboot.aicareyongin.domain.policy.client.AiSummaryRequest;
import com.springboot.aicareyongin.domain.policy.client.AiSummaryResponse;
import com.springboot.aicareyongin.domain.policy.client.MatchProfileReader;
import com.springboot.aicareyongin.domain.policy.dto.PolicyAiSummaryResponse;
import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.repository.PolicyRepository;
import com.springboot.aicareyongin.global.ai.AiServerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * AI 정책 요약. 요약(4대 핵심 정보)은 AI 서버가 미리 만들어 두고, 맞춤 팁은 가구 정보로 실시간 생성한다.
 * 가구 정보가 없어도 요약은 보여준다. AI 호출 중 DB 커넥션을 잡지 않도록 트랜잭션을 두지 않는다.
 */
@Service
@RequiredArgsConstructor
public class PolicySummaryService {

    private static final String SUMMARY_PATH = "/ai/policies/summary";

    private final PolicyRepository policyRepository;
    private final MatchProfileReader matchProfileReader;
    private final AiServerClient aiServerClient;

    public PolicyAiSummaryResponse getSummary(Long userId, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .filter(Policy::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 정책입니다."));
        AiSummaryRequest request = new AiSummaryRequest(policy.getSourceUrl(),
                matchProfileReader.readIfPresent(userId).orElse(null));
        AiSummaryResponse response = aiServerClient.post(SUMMARY_PATH, request, AiSummaryResponse.class);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버 응답이 올바르지 않습니다.");
        }
        return PolicyAiSummaryResponse.of(policy, response);
    }
}
