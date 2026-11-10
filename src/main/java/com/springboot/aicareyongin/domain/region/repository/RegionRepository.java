package com.springboot.aicareyongin.domain.region.repository;

import com.springboot.aicareyongin.domain.region.entity.Region;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByGuNameAndDongName(String guName, String dongName);

    List<Region> findAllByOrderByGuNameAscDongNameAsc();

    List<Region> findByGuNameOrderByDongNameAsc(String guName);

    // 기존 ID와 좌표·인구를 유지하며, 여러 서버가 동시에 실행해도 중복 삽입하지 않는다.
    @Modifying
    @Query(value = """
            INSERT INTO regions (gu_name, dong_name, region_type)
            VALUES (:guName, :dongName, :regionType)
            ON DUPLICATE KEY UPDATE region_id = region_id
            """, nativeQuery = true)
    int insertIfMissing(@Param("guName") String guName, @Param("dongName") String dongName,
                        @Param("regionType") String regionType);
}
