package com.springboot.aicareyongin.domain.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.refresh")
public record RefreshTokenProperties(Duration ttl) {
    public RefreshTokenProperties {
        if (ttl == null) ttl = Duration.ofDays(14);
        if (ttl.isNegative() || ttl.isZero() || ttl.getSeconds() < 1) {
            throw new IllegalArgumentException("Refresh token TTL must be at least one second");
        }
    }
}
