package com.springboot.aicareyongin.domain.policy.dto;

import com.springboot.aicareyongin.domain.policy.client.AiMatchResponse.AiPolicyMatch;
import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.MatchStatus;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import java.util.List;

public record PolicyMatchResponse(
        Long policyId,
        String title,
        PolicyCategory category,
        String providerOrg,
        MatchStatus status,
        int score,
        List<String> reasons,
        List<String> checkItems,
        List<String> labels,
        List<String> conditionTypes,
        List<String> targetChildren,
        List<String> notes,
        String applyPeriod) {

    public static PolicyMatchResponse of(Policy policy, AiPolicyMatch match) {
        return new PolicyMatchResponse(
                policy.getId(),
                policy.getTitle(),
                policy.getCategory(),
                policy.getProviderOrg(),
                MatchStatus.fromAi(match.status()),
                match.score(),
                nullToEmpty(match.reasons()),
                nullToEmpty(match.checkItems()),
                nullToEmpty(match.labels()),
                nullToEmpty(match.conditionTypes()),
                nullToEmpty(match.targetChildren()),
                nullToEmpty(match.notes()),
                match.applyPeriod());
    }

    public boolean isSpecialGroup() {
        return conditionTypes.contains("special_group");
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }
}
