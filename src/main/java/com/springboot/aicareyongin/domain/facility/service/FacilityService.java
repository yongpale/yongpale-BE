package com.springboot.aicareyongin.domain.facility.service;

import com.springboot.aicareyongin.domain.facility.dto.FacilityDetail;
import com.springboot.aicareyongin.domain.facility.dto.FacilityDetailResponse;
import com.springboot.aicareyongin.domain.facility.dto.FacilitySummaryResponse;
import com.springboot.aicareyongin.domain.facility.entity.Facility;
import com.springboot.aicareyongin.domain.facility.repository.FacilityRepository;
import com.springboot.aicareyongin.domain.facility.repository.FacilitySpecs;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final JsonMapper jsonMapper;

    public List<FacilitySummaryResponse> getFacilities(String facilityType, String guName, String dongName) {
        return facilityRepository.findAll(
                        FacilitySpecs.active()
                                .and(FacilitySpecs.subtypeEquals(facilityType))
                                .and(FacilitySpecs.guNameEquals(guName))
                                .and(FacilitySpecs.dongNameEquals(dongName)),
                        Sort.by("name"))
                .stream()
                .map(FacilitySummaryResponse::from)
                .toList();
    }

    public FacilityDetailResponse getFacility(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
                .filter(Facility::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 시설입니다."));
        return FacilityDetailResponse.of(facility, readDetail(facility));
    }

    private FacilityDetail readDetail(Facility facility) {
        if (facility.getRaw() == null) {
            return null;
        }
        return jsonMapper.readValue(facility.getRaw(), FacilityDetail.class);
    }
}
