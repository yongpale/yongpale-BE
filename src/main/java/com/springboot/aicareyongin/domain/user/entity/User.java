package com.springboot.aicareyongin.domain.user.entity;

import com.springboot.aicareyongin.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "terms_agreed_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime termsAgreedAt;

    @Column(name = "privacy_agreed_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime privacyAgreedAt;

    @Builder.Default
    @ColumnDefault("true")
    @Column(name = "notify_push", nullable = false)
    private boolean notifyPush = true;

    @Builder.Default
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 10)
    private String status = "ACTIVE";

    @Column(name = "last_login_at", columnDefinition = "timestamptz")
    private OffsetDateTime lastLoginAt;

    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    private OffsetDateTime deletedAt;
}
