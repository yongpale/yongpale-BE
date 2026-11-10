package com.springboot.aicareyongin.domain.household.repository;

import com.springboot.aicareyongin.domain.household.entity.HouseholdProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdProfileRepository extends JpaRepository<HouseholdProfile, Long> {
    @EntityGraph(attributePaths = "region")
    Optional<HouseholdProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
