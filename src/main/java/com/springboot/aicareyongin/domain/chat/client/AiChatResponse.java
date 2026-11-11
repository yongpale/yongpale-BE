package com.springboot.aicareyongin.domain.chat.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * AI 서버 POST /ai/chat 응답.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiChatResponse(
        String model,
        @JsonProperty("profile_label") String profileLabel,
        boolean grounded,
        boolean generated,
        String intro,
        List<Item> items,
        List<Source> sources,
        @JsonProperty("related_policies") List<PolicyLink> relatedPolicies,
        @JsonProperty("related_facilities") List<FacilityLink> relatedFacilities,
        @JsonProperty("suggested_questions") List<String> suggestedQuestions) {

    /** 번호 목록 한 줄 (예: 신청처, 제출 서류, 접수 기간) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String title, String text) {
    }

    /**
     * @param type policy / facility
     * @param id   정책: 정부24 서비스ID, 시설: 시설 CSV id
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(
            String type,
            String id,
            String title,
            String name,
            String url,
            @JsonProperty("updated_at") String updatedAt) {
    }

    /** id: 정부24 서비스ID */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolicyLink(String id, String name, String status) {
    }

    /** id: 시설 CSV id */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FacilityLink(String id, String name) {
    }
}
