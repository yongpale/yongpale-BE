package com.springboot.aicareyongin.domain.caregap.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.springboot.aicareyongin.domain.policy.client.MatchProfile;

/**
 * AI 서버 POST /ai/care-gap 요청.
 *
 * @param childIndex 분석할 자녀 (profile.childGrades 순서, 0부터)
 * @param period     semester / vacation
 * @param schoolEnd  하교 시각. null이면 AI가 학년별 기본값 사용
 * @param careTime   돌봄 필요 시간. null이면 profile.careTime 사용
 */
public record AiCareGapRequest(
        MatchProfile profile,
        @JsonProperty("child_index") int childIndex,
        String period,
        @JsonProperty("school_end") String schoolEnd,
        @JsonProperty("care_time") MatchProfile.CareTime careTime,
        @JsonProperty("max_results") int maxResults) {
}
