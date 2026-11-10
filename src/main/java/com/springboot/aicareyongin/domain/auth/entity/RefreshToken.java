package com.springboot.aicareyongin.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_refresh_tokens", indexes = @Index(name = "idx_refresh_tokens_session", columnList = "session_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AuthSession session;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    @Column(name = "consumed_at", columnDefinition = "datetime(6)")
    private Instant consumedAt;

    public RefreshToken(String tokenHash, AuthSession session, Instant createdAt) {
        this.tokenHash = tokenHash;
        this.session = session;
        this.createdAt = createdAt;
    }

    public void consume(Instant now) {
        consumedAt = now;
    }
}
