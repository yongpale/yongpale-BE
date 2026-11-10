package com.springboot.aicareyongin.domain.policy.dto;

import com.springboot.aicareyongin.domain.policy.entity.Policy;
import java.util.List;

public record RequiredDocumentsResponse(
        Long policyId,
        String title,
        List<String> requiredDocuments) {

    public static RequiredDocumentsResponse of(Policy policy, List<String> requiredDocuments) {
        return new RequiredDocumentsResponse(policy.getId(), policy.getTitle(), requiredDocuments);
    }
}
