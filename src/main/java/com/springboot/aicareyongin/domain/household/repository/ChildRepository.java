package com.springboot.aicareyongin.domain.household.repository;

import com.springboot.aicareyongin.domain.household.entity.Child;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChildRepository extends JpaRepository<Child, Long> {
    List<Child> findAllByProfileIdOrderByIdAsc(Long profileId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Child child where child.profile.id = :profileId")
    void deleteAllByProfileId(@Param("profileId") Long profileId);
}
