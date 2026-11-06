package com.springboot.aicareyongin.domain.facility.repository;

import com.springboot.aicareyongin.domain.facility.entity.Facility;
import org.springframework.data.jpa.domain.Specification;

/**
 * 시설 목록 필터 조건. 값이 null이면 조건을 걸지 않는다.
 */
public final class FacilitySpecs {

    private FacilitySpecs() {
    }

    public static Specification<Facility> active() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Facility> subtypeEquals(String subtype) {
        return equalsIfPresent("subtype", subtype);
    }

    public static Specification<Facility> guNameEquals(String guName) {
        return equalsIfPresent("guName", guName);
    }

    public static Specification<Facility> dongNameEquals(String dongName) {
        return equalsIfPresent("dongName", dongName);
    }

    private static Specification<Facility> equalsIfPresent(String field, String value) {
        if (value == null || value.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }
}
