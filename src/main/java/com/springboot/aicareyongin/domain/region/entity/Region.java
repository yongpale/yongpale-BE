package com.springboot.aicareyongin.domain.region.entity;

import com.springboot.aicareyongin.domain.region.enums.RegionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "regions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"gu_name", "dong_name"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id")
    private Long id;

    @Column(name = "gu_name", nullable = false, length = 20)
    private String guName;

    @Column(name = "dong_name", nullable = false, length = 30)
    private String dongName;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_type", nullable = false, length = 10)
    private RegionType regionType;

    @Column(name = "center_lat", precision = 10, scale = 7)
    private BigDecimal centerLat;

    @Column(name = "center_lng", precision = 10, scale = 7)
    private BigDecimal centerLng;

    @Column(name = "child_population")
    private Integer childPopulation;

    @Column(name = "population_base_date")
    private LocalDate populationBaseDate;
}
