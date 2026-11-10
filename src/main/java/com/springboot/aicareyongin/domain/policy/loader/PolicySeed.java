package com.springboot.aicareyongin.domain.policy.loader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 레포 data/processed/policies.json 한 건. 적재에 필요한 필드만 읽는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicySeed(String name, String agency, Texts texts, Apply apply, Source source, Match match) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Texts(String target, String benefits,
                        @JsonProperty("how_to_apply") String howToApply,
                        String documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Apply(@JsonProperty("online_url") String onlineUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(String url, @JsonProperty("updated_at") String updatedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Match(String category,
                        @JsonProperty("grade_min") Short gradeMin,
                        @JsonProperty("grade_max") Short gradeMax) {
    }
}
