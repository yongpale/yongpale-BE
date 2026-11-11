package com.springboot.aicareyongin.domain.policy.entity;

import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.global.entity.BaseCreatedTimeEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 필요 서류 체크리스트에서 회원이 "준비 완료"로 체크한 서류. 행이 있으면 체크, 없으면 미체크.
 * document_code는 required-documents.json의 서류 코드(예: RESIDENT_REGISTRATION)이고,
 * 서류 데이터가 없는 정책은 원문 순서로 만든 ORIGINAL_1, ORIGINAL_2 …를 쓴다.
 */
@Entity
@Table(name = "policy_document_checks",
        uniqueConstraints = @UniqueConstraint(name = "uk_policy_document_checks",
                columnNames = {"user_id", "policy_id", "document_code"}),
        indexes = @Index(name = "idx_policy_document_checks_policy", columnList = "policy_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyDocumentCheck extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "document_code", nullable = false, length = 40)
    private String documentCode;
}
