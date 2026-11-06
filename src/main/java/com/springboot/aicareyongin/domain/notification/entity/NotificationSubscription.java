package com.springboot.aicareyongin.domain.notification.entity;

import com.springboot.aicareyongin.domain.notification.enums.NotificationChannel;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.global.entity.BaseCreatedTimeEntity;
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
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_subscriptions", indexes = {
        @Index(name = "idx_notification_subscriptions_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"channel", "endpoint"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationSubscription extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(name = "endpoint", nullable = false, columnDefinition = "text")
    private String endpoint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keys", columnDefinition = "jsonb")
    private String keys;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Builder.Default
    @ColumnDefault("true")
    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "fail_count", nullable = false)
    private Short failCount = (short) 0;

    @Column(name = "last_success_at", columnDefinition = "timestamptz")
    private OffsetDateTime lastSuccessAt;
}
