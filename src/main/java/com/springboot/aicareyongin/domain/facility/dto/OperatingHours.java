package com.springboot.aicareyongin.domain.facility.dto;

/**
 * 운영시간. 시간은 "HH:MM" 문자열이라 문자열 비교로 대소 비교가 된다.
 */
public record OperatingHours(
        String open,
        String close
) {

    public static OperatingHours of(String open, String close) {
        if (open == null || close == null) {
            return null;
        }
        return new OperatingHours(open, close);
    }
}
