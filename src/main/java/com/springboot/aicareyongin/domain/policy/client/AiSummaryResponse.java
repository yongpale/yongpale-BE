package com.springboot.aicareyongin.domain.policy.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * AI 서버 POST /ai/policies/summary 응답.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiSummaryResponse(
        String status,
        List<Item> summary,
        @JsonProperty("summary_generated") boolean summaryGenerated,
        @JsonProperty("household_documents") List<HouseholdDocument> householdDocuments,
        Tip tip,
        Apply apply,
        List<Source> sources,
        @JsonProperty("info_date") String infoDate) {

    /** 신청 자격 / 지원 혜택 / 필요 서류 / 신청 기간 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String title, String text, List<String> evidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HouseholdDocument(
            @JsonProperty("household_type") String householdType,
            @JsonProperty("detail_type") String detailType,
            String documents,
            @JsonProperty("how_to_get") String howToGet) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tip(String text, boolean generated) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Apply(
            @JsonProperty("how_to_apply") String howToApply,
            @JsonProperty("deadline_type") String deadlineType,
            String deadline,
            String reception,
            String contact,
            @JsonProperty("online_url") String onlineUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(
            String name,
            String url,
            @JsonProperty("updated_at") String updatedAt) {
    }
}
