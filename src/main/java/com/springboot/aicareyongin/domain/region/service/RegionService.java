package com.springboot.aicareyongin.domain.region.service;

import com.springboot.aicareyongin.domain.region.dto.RegionResponse;
import com.springboot.aicareyongin.domain.region.repository.RegionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {
    private final RegionRepository regions;

    public List<RegionResponse> getRegions(String guName) {
        var found = guName == null || guName.isBlank()
                ? regions.findAllByOrderByGuNameAscDongNameAsc()
                : regions.findByGuNameOrderByDongNameAsc(guName.strip());
        return found.stream().map(RegionResponse::from).toList();
    }
}
