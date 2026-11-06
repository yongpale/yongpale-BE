package com.springboot.aicareyongin.domain.facility.controller;

import com.springboot.aicareyongin.domain.facility.dto.FacilityDetailResponse;
import com.springboot.aicareyongin.domain.facility.dto.FacilitySummaryResponse;
import com.springboot.aicareyongin.domain.facility.service.FacilityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    /**
     * 시설 목록 조회. 모든 필터는 선택이다.
     *
     * @param type 다함께돌봄센터 / 지역아동센터
     * @param gu   구 이름 (예: 기흥구)
     * @param dong 읍·면·동 이름 (예: 구갈동)
     */
    @GetMapping
    public List<FacilitySummaryResponse> getFacilities(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String gu,
            @RequestParam(required = false) String dong) {
        return facilityService.getFacilities(type, gu, dong);
    }

    @GetMapping("/{facilityId}")
    public FacilityDetailResponse getFacility(@PathVariable Long facilityId) {
        return facilityService.getFacility(facilityId);
    }
}
