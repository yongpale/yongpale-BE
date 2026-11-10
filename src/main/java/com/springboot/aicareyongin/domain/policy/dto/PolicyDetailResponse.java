package com.springboot.aicareyongin.domain.policy.dto;

import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import java.time.LocalDate;
import java.util.List;

public record PolicyDetailResponse(
        Long policyId,
        String title,
        PolicyCategory category,
        String providerOrg,
        String targetText,
        String benefitText,
        Integer benefitAmount,
        List<String> requiredDocuments,
        String applyMethodText,
        Short targetGradeMin,
        Short targetGradeMax,
        String applyUrl,
        String sourceUrl,
        String status,
        LocalDate asOfDate) {

    public static PolicyDetailResponse of(Policy policy, List<String> requiredDocuments) {
        return new PolicyDetailResponse(
                policy.getId(),
                policy.getTitle(),
                policy.getCategory(),
                policy.getProviderOrg(),
                policy.getTargetText(),
                policy.getBenefitText(),
                policy.getBenefitAmount(),
                requiredDocuments,
                policy.getApplyMethodText(),
                policy.getTargetGradeMin(),
                policy.getTargetGradeMax(),
                policy.getApplyUrl(),
                policy.getSourceUrl(),
                policy.getStatus(),
                policy.getAsOfDate());
    }
}
