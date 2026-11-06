package com.springboot.aicareyongin.domain.facility.dto;

import com.springboot.aicareyongin.domain.facility.entity.Facility;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 시설 상세 응답. 현원 데이터가 없어 정원(capacity)만 내려준다.
 */
public record FacilityDetailResponse(
        Long facilityId,
        String name,
        String facilityType,
        String guName,
        String dongName,
        String address,
        String phone,
        Integer capacity,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate dataBaseDate,
        FacilityDetail detail
) {

    public static FacilityDetailResponse of(Facility facility, FacilityDetail detail) {
        return new FacilityDetailResponse(
                facility.getId(),
                facility.getName(),
                facility.getSubtype(),
                facility.getGuName(),
                facility.getDongName(),
                facility.getAddress(),
                facility.getPhone(),
                facility.getCapacity(),
                facility.getLatitude(),
                facility.getLongitude(),
                facility.getDataBaseDate(),
                detail
        );
    }
}
