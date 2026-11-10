package com.springboot.aicareyongin.domain.region.controller;

import com.springboot.aicareyongin.domain.region.dto.RegionResponse;
import com.springboot.aicareyongin.domain.region.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/regions")
@Tag(name = "Regions", description = "가족 정보 입력용 거주 지역 목록")
public class RegionController {
    private final RegionService service;

    @GetMapping
    @Operation(summary = "거주 지역 조회", description = "구를 생략하면 전체 목록을 반환합니다. JWT 인증이 필요합니다.")
    public List<RegionResponse> getRegions(@RequestParam(required = false) String guName) {
        return service.getRegions(guName);
    }
}
