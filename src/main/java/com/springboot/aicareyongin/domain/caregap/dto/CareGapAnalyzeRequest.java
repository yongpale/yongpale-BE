package com.springboot.aicareyongin.domain.caregap.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.springboot.aicareyongin.domain.caregap.enums.CarePeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

/**
 * 돌봄 공백 분석 요청. 모든 항목은 선택이다.
 *
 * @param childId           분석할 자녀. 생략하면 학년이 가장 낮은 초등학생 자녀. 다른 자녀 ID로 다시 요청하면 그 자녀로 분석한다.
 * @param period            SEMESTER(학기 중, 기본) / VACATION(방학 중)
 * @param schoolEndTime     하교 시각 (학기 중). 생략하면 학년별 기본값
 * @param careNeedStartTime 돌봄 필요 시작 시각. 생략하면 가구 정보의 값
 * @param careNeedEndTime   돌봄 필요 종료 시각. 생략하면 가구 정보의 값
 */
public record CareGapAnalyzeRequest(
        @Schema(example = "1", nullable = true) Long childId,
        @Schema(example = "SEMESTER", nullable = true) CarePeriod period,
        @Pattern(regexp = TIME, message = "하교 시각은 HH:mm 형식이어야 합니다.")
        @Schema(example = "13:00", nullable = true) String schoolEndTime,
        @Pattern(regexp = TIME, message = "돌봄 시작 시각은 HH:mm 형식이어야 합니다.")
        @Schema(example = "13:00", nullable = true) String careNeedStartTime,
        @Pattern(regexp = TIME, message = "돌봄 종료 시각은 HH:mm 형식이어야 합니다.")
        @Schema(example = "18:00", nullable = true) String careNeedEndTime) {

    static final String TIME = "([01][0-9]|2[0-3]):[0-5][0-9]";

    @AssertTrue(message = "돌봄 시작·종료 시각을 함께 입력하고, 종료 시각은 시작 시각보다 늦어야 합니다.")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isCareNeedTimeValid() {
        if (careNeedStartTime == null && careNeedEndTime == null) return true;
        if (careNeedStartTime == null || careNeedEndTime == null) return false;
        return careNeedStartTime.matches(TIME) && careNeedEndTime.matches(TIME)
                && careNeedStartTime.compareTo(careNeedEndTime) < 0;
    }

    public CarePeriod periodOrDefault() {
        return period == null ? CarePeriod.SEMESTER : period;
    }

    public boolean hasCareNeedTime() {
        return careNeedStartTime != null && careNeedEndTime != null;
    }
}
