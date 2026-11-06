package com.springboot.aicareyongin.domain.facility.repository;

import com.springboot.aicareyongin.domain.facility.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FacilityRepository extends JpaRepository<Facility, Long>, JpaSpecificationExecutor<Facility> {
}
