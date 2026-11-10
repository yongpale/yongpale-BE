package com.springboot.aicareyongin.domain.auth.config;

import com.springboot.aicareyongin.domain.auth.service.JwtService;
import com.springboot.aicareyongin.domain.user.entity.User;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {
    private final JwtConfig config = new JwtConfig();
    private final JwtProperties properties = new JwtProperties(
            "test-secret-with-at-least-32-bytes-for-tests", "test-issuer", Duration.ofMinutes(30));
    private final User user = User.builder().id(7L).nickname("회원").build();

    @Test
    void issuesAndValidatesTokenWithMemberIdAndExpiry() {
        var key = config.jwtSigningKey(properties);
        var response = new JwtService(config.jwtEncoder(key), properties, Clock.systemUTC()).issue(user, "test-session-id");
        var decoded = config.jwtDecoder(key, properties).decode(response.accessToken());
        assertThat(decoded.getSubject()).isEqualTo("7");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("test-issuer");
        assertThat(Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(response.user().termsAgreementRequired()).isTrue();
        assertThat(response.expiresIn()).isEqualTo(1800);
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        var other = new JwtProperties("another-secret-with-at-least-32-bytes", "test-issuer", Duration.ofMinutes(30));
        var token = new JwtService(config.jwtEncoder(config.jwtSigningKey(other)), other, Clock.systemUTC()).issue(user, "test-session-id");
        assertThatThrownBy(() -> config.jwtDecoder(config.jwtSigningKey(properties), properties).decode(token.accessToken()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        var oldClock = Clock.offset(Clock.systemUTC(), Duration.ofHours(-2));
        var key = config.jwtSigningKey(properties);
        var token = new JwtService(config.jwtEncoder(key), properties, oldClock).issue(user, "test-session-id");
        assertThatThrownBy(() -> config.jwtDecoder(key, properties).decode(token.accessToken()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWrongIssuer() {
        var key = config.jwtSigningKey(properties);
        var other = new JwtProperties(properties.secret(), "other-issuer", Duration.ofMinutes(30));
        var token = new JwtService(config.jwtEncoder(key), other, Clock.systemUTC()).issue(user, "test-session-id");
        assertThatThrownBy(() -> config.jwtDecoder(key, properties).decode(token.accessToken()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWeakSigningKey() {
        assertThatThrownBy(() -> config.jwtSigningKey(new JwtProperties("short", "issuer", Duration.ofMinutes(30))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
