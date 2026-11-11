package com.springboot.aicareyongin.domain.caregap.enums;

/**
 * 돌봄 공백 분석 기간. AI 서버의 semester / vacation에 대응한다.
 */
public enum CarePeriod {
    SEMESTER("semester"),  // 학기 중: 하교 후부터 공백
    VACATION("vacation");  // 방학 중: 돌봄 필요 시간 전체가 공백

    private final String aiValue;

    CarePeriod(String aiValue) {
        this.aiValue = aiValue;
    }

    public String aiValue() {
        return aiValue;
    }
}
