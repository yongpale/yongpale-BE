package com.springboot.aicareyongin.domain.facility.entity;

import com.springboot.aicareyongin.domain.facility.enums.FacilityType;
import com.springboot.aicareyongin.domain.facility.enums.SourceType;
import com.springboot.aicareyongin.domain.region.entity.Region;
import com.springboot.aicareyongin.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "facilities", indexes = {
        @Index(name = "idx_facilities_type", columnList = "facility_type"),
        @Index(name = "idx_facilities_region", columnList = "region_id"),
        @Index(name = "idx_facilities_geo", columnList = "latitude, longitude")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"source_type", "source_ref_id"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Facility extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "facility_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", nullable = false, length = 15)
    private FacilityType facilityType;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "subtype", length = 50)
    private String subtype;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(name = "source_ref_id", nullable = false, length = 100)
    private String sourceRefId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "gu_name", length = 20)
    private String guName;

    @Column(name = "dong_name", length = 30)
    private String dongName;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Builder.Default
    @ColumnDefault("'NONE'")
    @Column(name = "geocode_status", nullable = false, length = 10)
    private String geocodeStatus = "NONE";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "current_enrollment")
    private Integer currentEnrollment;

    @Column(name = "has_night_extension")
    private Boolean hasNightExtension;

    @Column(name = "has_holiday_care")
    private Boolean hasHolidayCare;

    @Column(name = "is_public_type")
    private Boolean isPublicType;

    @Column(name = "data_base_date")
    private LocalDate dataBaseDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw", columnDefinition = "json")
    private String raw;

    @Builder.Default
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** 좌표가 비어 있을 때만 채운다. 이미 있는 좌표는 덮어쓰지 않는다. */
    public boolean fillCoordinatesIfEmpty(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null || (this.latitude != null && this.longitude != null)) {
            return false;
        }
        this.latitude = latitude;
        this.longitude = longitude;
        return true;
    }
}
