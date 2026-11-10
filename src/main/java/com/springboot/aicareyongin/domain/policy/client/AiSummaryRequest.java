package com.springboot.aicareyongin.domain.policy.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 POST /ai/policies/summary 요청.
 *
 * @param sourceUrl 백엔드 policies.source_url. AI 서버가 이 값(svcId)으로 정책을 찾는다.
 * @param profile   매칭 프로필. 가구 정보가 없으면 null이고, 이때 AI는 맞춤 팁 없이 요약만 준다.
 */
public record AiSummaryRequest(
        @JsonProperty("source_url") String sourceUrl,
        MatchProfile profile) {
}
