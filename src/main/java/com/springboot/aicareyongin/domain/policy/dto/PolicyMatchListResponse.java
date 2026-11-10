package com.springboot.aicareyongin.domain.policy.dto;

import com.springboot.aicareyongin.domain.policy.enums.MatchStatus;
import java.util.List;

/**
 * 맞춤 정책 목록. 건수는 백엔드 정책과 연결된 결과로 다시 센다.
 *
 * @param total               해당 가능 + 확인 필요 (홈 화면 "총 N건")
 * @param specialGroupCount   다문화·장애 등 특정 가구 대상이라 확인이 필요한 정책 수
 * @param totalWithoutSpecial 특정 가구 대상 정책을 뺀 수
 */
public record PolicyMatchListResponse(
        int total,
        int eligibleCount,
        int checkCount,
        int specialGroupCount,
        int totalWithoutSpecial,
        List<PolicyMatchResponse> policies) {

    public static PolicyMatchListResponse of(List<PolicyMatchResponse> policies) {
        int eligible = count(policies, MatchStatus.ELIGIBLE);
        int check = count(policies, MatchStatus.CHECK);
        int special = (int) policies.stream().filter(PolicyMatchResponse::isSpecialGroup).count();
        return new PolicyMatchListResponse(eligible + check, eligible, check, special,
                eligible + check - special, policies);
    }

    private static int count(List<PolicyMatchResponse> policies, MatchStatus status) {
        return (int) policies.stream().filter(p -> p.status() == status).count();
    }
}
