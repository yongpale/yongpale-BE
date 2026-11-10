package com.springboot.aicareyongin.domain.household.entity;

import com.springboot.aicareyongin.domain.region.entity.Region;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "household_profiles", indexes = {
        @Index(name = "idx_profiles_session", columnList = "anon_session_id")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HouseholdProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "anon_session_id", length = 64)
    private String anonSessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "is_dual_income", nullable = false)
    private boolean isDualIncome = false;

    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "is_single_parent", nullable = false)
    private boolean isSingleParent = false;

    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "is_multi_child", nullable = false)
    private boolean isMultiChild = false;

    @Column(name = "care_need_start_time")
    private LocalTime careNeedStartTime;

    @Column(name = "care_need_end_time")
    private LocalTime careNeedEndTime;

    public void updateCareNeedTime(LocalTime startTime, LocalTime endTime) {
        this.careNeedStartTime = startTime;
        this.careNeedEndTime = endTime;
    }

    public void updateRegion(Region region) {
        this.region = region;
    }

    public void updateSituations(boolean dualIncome, boolean singleParent, boolean multiChild) {
        this.isDualIncome = dualIncome;
        this.isSingleParent = singleParent;
        this.isMultiChild = multiChild;
    }
}
