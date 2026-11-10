package com.springboot.aicareyongin.domain.policy.enums;

/**
 * 정책 매칭 판정. AI 서버의 eligible / check / ineligible에 대응한다.
 */
public enum MatchStatus {
    ELIGIBLE,    // 해당 가능
    CHECK,       // 조건 확인 필요
    INELIGIBLE;  // 해당 안 됨

    public static MatchStatus fromAi(String status) {
        return switch (status) {
            case "eligible" -> ELIGIBLE;
            case "check" -> CHECK;
            case "ineligible" -> INELIGIBLE;
            default -> throw new IllegalArgumentException("알 수 없는 매칭 판정입니다: " + status);
        };
    }
}
