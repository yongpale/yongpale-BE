package com.springboot.aicareyongin.domain.caregap.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * AI 서버 POST /ai/care-gap 응답.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiCareGapResponse(
        String period,
        @JsonProperty("care_time") TimeRange careTime,
        @JsonProperty("school_end") String schoolEnd,
        List<TimeRange> gap,
        @JsonProperty("gap_minutes") int gapMinutes,
        List<Plan> plans,
        @JsonProperty("related_policies") List<RelatedPolicy> relatedPolicies,
        List<String> notes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimeRange(String start, String end, int minutes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Plan(
            List<Facility> facilities,
            @JsonProperty("covered_minutes") int coveredMinutes,
            @JsonProperty("coverage_rate") double coverageRate,
            List<TimeRange> remaining,
            List<String> reasons) {
    }

    /** id는 시설 CSV의 id (백엔드 facilities.source_ref_id) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Facility(
            String id,
            String name,
            TimeRange hours,
            TimeRange covers,
            Map<String, Object> fee,
            Boolean meal,
            Boolean snack,
            Boolean pickup) {
    }

    /** id는 정부24 서비스ID (백엔드 policies.source_url의 svcId) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelatedPolicy(String id, String name, String status, String reason) {
    }
}
