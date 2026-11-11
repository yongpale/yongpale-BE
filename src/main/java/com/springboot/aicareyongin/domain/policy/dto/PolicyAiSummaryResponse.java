package com.springboot.aicareyongin.domain.policy.dto;

import com.springboot.aicareyongin.domain.policy.client.AiSummaryResponse;
import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.MatchStatus;
import java.util.List;

/**
 * AI 정책 요약.
 *
 * @param status           내 가구 기준 판정. 가구 정보가 없으면 null
 * @param summary          4대 핵심 정보 (신청 자격, 지원 혜택, 필요 서류, 신청 기간 순)
 * @param summaryGenerated false면 AI 요약이 없어 원문 발췌로 대신한 것
 * @param householdDocuments 가정 유형별 입증서류 (아이돌봄 서비스만)
 * @param tip              가구 맞춤 팁. 가구 정보가 없으면 null
 * @param infoDate         정보 기준일 (원본 수정일)
 */
public record PolicyAiSummaryResponse(
        Long policyId,
        String title,
        MatchStatus status,
        List<AiSummaryResponse.Item> summary,
        boolean summaryGenerated,
        List<AiSummaryResponse.HouseholdDocument> householdDocuments,
        AiSummaryResponse.Tip tip,
        AiSummaryResponse.Apply apply,
        List<AiSummaryResponse.Source> sources,
        String infoDate) {

    public static PolicyAiSummaryResponse of(Policy policy, AiSummaryResponse ai) {
        return new PolicyAiSummaryResponse(
                policy.getId(),
                policy.getTitle(),
                ai.status() == null ? null : MatchStatus.fromAi(ai.status()),
                ai.summary() == null ? List.of() : ai.summary(),
                ai.summaryGenerated(),
                ai.householdDocuments() == null ? List.of() : ai.householdDocuments(),
                ai.tip(),
                ai.apply(),
                ai.sources() == null ? List.of() : ai.sources(),
                ai.infoDate());
    }
}
