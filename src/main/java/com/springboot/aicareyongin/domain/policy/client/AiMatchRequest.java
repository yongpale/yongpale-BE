package com.springboot.aicareyongin.domain.policy.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 POST /ai/policies/match 요청.
 *
 * @param profile           백엔드가 정한 매칭 프로필 (자녀 수·학년, 맞벌이, 한부모, 다자녀, 돌봄 필요 시간)
 * @param includeIneligible 해당 안 됨 정책도 받을지 (단건 매칭에서 이유를 보여줄 때 true)
 */
public record AiMatchRequest(
        MatchProfile profile,
        @JsonProperty("include_ineligible") boolean includeIneligible) {
}
