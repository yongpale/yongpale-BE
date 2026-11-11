package com.springboot.aicareyongin.domain.policy.enums;

import lombok.extern.slf4j.Slf4j;

/**
 * 정책 매칭 판정. AI 서버의 eligible / check / ineligible에 대응한다.
 */
@Slf4j
public enum MatchStatus {
    ELIGIBLE,    // 해당 가능
    CHECK,       // 조건 확인 필요
    INELIGIBLE;  // 해당 안 됨

    /**
     * AI 서버 값으로 변환한다. 값이 없거나 모르는 값이면 null.
     * AI 서버에 판정 값이 추가돼도 응답 전체가 실패하지 않도록 예외를 던지지 않는다.
     */
    public static MatchStatus fromAi(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "eligible" -> ELIGIBLE;
            case "check" -> CHECK;
            case "ineligible" -> INELIGIBLE;
            default -> {
                log.warn("알 수 없는 AI 매칭 판정입니다: {}", status);
                yield null;
            }
        };
    }
}
