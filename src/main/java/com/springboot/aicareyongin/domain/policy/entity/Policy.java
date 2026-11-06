package com.springboot.aicareyongin.domain.policy.entity;

import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import com.springboot.aicareyongin.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
@Table(name = "policies", indexes = {
        @Index(name = "idx_policies_active", columnList = "is_active, status")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Policy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PolicyCategory category;

    @Column(name = "provider_org", length = 100)
    private String providerOrg;

    @Column(name = "target_text", columnDefinition = "text")
    private String targetText;

    @Column(name = "benefit_text", columnDefinition = "text")
    private String benefitText;

    @Column(name = "benefit_amount")
    private Integer benefitAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_documents", nullable = false, columnDefinition = "jsonb")
    private String requiredDocuments;

    @Column(name = "apply_method_text", columnDefinition = "text")
    private String applyMethodText;

    @Column(name = "target_grade_min")
    private Short targetGradeMin;

    @Column(name = "target_grade_max")
    private Short targetGradeMax;

    @Column(name = "expansion_note", length = 300)
    private String expansionNote;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "target_region_ids", columnDefinition = "bigint[]")
    private Long[] targetRegionIds;

    @Column(name = "apply_url", length = 500)
    private String applyUrl;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Builder.Default
    @ColumnDefault("'OPEN'")
    @Column(name = "status", nullable = false, length = 10)
    private String status = "OPEN";

    @Builder.Default
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
