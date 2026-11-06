package com.springboot.aicareyongin.domain.knowledge.entity;

import com.springboot.aicareyongin.domain.policy.entity.Policy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "knowledge_documents", indexes = {
        @Index(name = "idx_knowledge_documents_policy", columnList = "policy_id")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "source_org", length = 100)
    private String sourceOrg;

    @Builder.Default
    @ColumnDefault("'MANUAL'")
    @Column(name = "doc_type", nullable = false, length = 10)
    private String docType = "MANUAL";

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Builder.Default
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 10)
    private String status = "ACTIVE";

    @CreationTimestamp
    @ColumnDefault("now()")
    @Column(name = "fetched_at", nullable = false, columnDefinition = "datetime(6)")
    private OffsetDateTime fetchedAt;
}
