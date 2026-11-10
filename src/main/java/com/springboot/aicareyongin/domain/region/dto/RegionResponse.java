package com.springboot.aicareyongin.domain.region.dto;

import com.springboot.aicareyongin.domain.region.entity.Region;
import com.springboot.aicareyongin.domain.region.enums.RegionType;

public record RegionResponse(Long id, String guName, String dongName, RegionType regionType) {
    public static RegionResponse from(Region region) {
        return new RegionResponse(region.getId(), region.getGuName(), region.getDongName(), region.getRegionType());
    }
}
