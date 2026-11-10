package com.springboot.aicareyongin.domain.policy.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * AI 서버 POST /ai/policies/match 응답. 정책은 AI가 정렬한 순서(해당 가능 → 확인 필요, 점수 높은 순)로 온다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiMatchResponse(List<AiPolicyMatch> policies) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiPolicyMatch(
            String id,
            String name,
            String status,
            int score,
            List<String> reasons,
            @JsonProperty("check_items") List<String> checkItems,
            List<String> labels,
            @JsonProperty("condition_types") List<String> conditionTypes,
            @JsonProperty("target_children") List<String> targetChildren,
            List<String> notes,
            @JsonProperty("apply_period") String applyPeriod,
            Source source) {
    }

    // 백엔드 policies.source_url과 같은 값이라 정책을 연결하는 키로 쓴다.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(String url) {
    }
}
