package com.springboot.aicareyongin.domain.facility.dto;

import com.springboot.aicareyongin.domain.facility.entity.Facility;
import java.math.BigDecimal;

/**
 * 시설 목록·지도 마커용 응답. 좌표가 없으면 latitude, longitude는 null이고
 * 프론트에서 address로 좌표를 변환한다.
 */
public record FacilitySummaryResponse(
        Long facilityId,
        String name,
        String facilityType,
        String guName,
        String dongName,
        String address,
        String phone,
        Integer capacity,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public static FacilitySummaryResponse from(Facility facility) {
        return new FacilitySummaryResponse(
                facility.getId(),
                facility.getName(),
                facility.getSubtype(),
                facility.getGuName(),
                facility.getDongName(),
                facility.getAddress(),
                facility.getPhone(),
                facility.getCapacity(),
                facility.getLatitude(),
                facility.getLongitude()
        );
    }
}
