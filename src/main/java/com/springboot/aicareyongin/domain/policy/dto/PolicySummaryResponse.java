package com.springboot.aicareyongin.domain.policy.dto;

import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import java.time.LocalDate;

public record PolicySummaryResponse(
        Long policyId,
        String title,
        PolicyCategory category,
        String providerOrg,
        Short targetGradeMin,
        Short targetGradeMax,
        String applyUrl,
        String status,
        LocalDate asOfDate) {

    public static PolicySummaryResponse from(Policy policy) {
        return new PolicySummaryResponse(
                policy.getId(),
                policy.getTitle(),
                policy.getCategory(),
                policy.getProviderOrg(),
                policy.getTargetGradeMin(),
                policy.getTargetGradeMax(),
                policy.getApplyUrl(),
                policy.getStatus(),
                policy.getAsOfDate());
    }
}
