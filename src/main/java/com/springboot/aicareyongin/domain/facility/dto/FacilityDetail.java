package com.springboot.aicareyongin.domain.facility.dto;

import java.util.List;

/**
 * Facility 엔티티 컬럼에 없는 시설 정보. facilities.raw(JSON)에 저장한다.
 */
public record FacilityDetail(
        Integer ageMin,
        Integer ageMax,
        boolean allAges,
        OperatingHours semesterHours,
        OperatingHours vacationHours,
        OperatingHours saturdayHours,
        OperatingHours tempSemesterHours,
        OperatingHours tempVacationHours,
        String feeType,
        String feeUnit,
        Integer feeAmount,
        Boolean meal,
        Boolean snack,
        Boolean pickup,
        Integer tempCapacity,
        List<String> programs,
        String applicationInfo,
        String source
) {
}
