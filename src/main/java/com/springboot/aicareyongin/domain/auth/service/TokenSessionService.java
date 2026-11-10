package com.springboot.aicareyongin.domain.auth.service;

import com.springboot.aicareyongin.domain.auth.config.RefreshTokenProperties;
import com.springboot.aicareyongin.domain.auth.dto.TokenResponse;
import com.springboot.aicareyongin.domain.auth.entity.AuthSession;
import com.springboot.aicareyongin.domain.auth.entity.RefreshToken;
import com.springboot.aicareyongin.domain.auth.repository.AuthSessionRepository;
import com.springboot.aicareyongin.domain.auth.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class TokenSessionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    @Transactional
    public TokenResponse login(Long userId) {
        var user = authService.getActiveUser(userId);
        var now = clock.instant();
        var session = sessions.save(new AuthSession(user, now, now.plus(properties.ttl())));
        return issuePair(session);
    }

    // 이미 사용한 토큰의 재사용을 감지했을 때도 폐기 상태를 커밋한다.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenResponse refresh(String rawToken) {
        AuthSession session = sessions.findByIdForUpdate(sessionId(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        RefreshToken token = verifiedToken(rawToken, session);
        var now = clock.instant();
        if (token.getConsumedAt() != null || !session.isActive(now) || !session.getUser().isLoginAllowed()) {
            session.revoke(now);
            throw new InvalidRefreshTokenException();
        }
        token.consume(now);
        return issuePair(session);
    }

    @Transactional
    public void logout(String rawToken) {
        String id;
        try {
            id = sessionId(rawToken);
        } catch (InvalidRefreshTokenException exception) {
            return;
        }
        var session = sessions.findByIdForUpdate(id).orElse(null);
        if (session == null) return;
        var token = refreshTokens.findById(hash(rawToken)).orElse(null);
        if (token != null && token.getSession().getId().equals(session.getId())) {
            // 교체되었거나 만료된 토큰으로도 해당 로그인을 종료할 수 있다.
            session.revoke(clock.instant());
        }
    }

    @Transactional(readOnly = true)
    public void requireActiveSession(Jwt jwt) {
        String sessionId = jwt.getClaimAsString("sid");
        if (sessionId == null || jwt.getId() == null || jwt.getExpiresAt() == null) {
            throw new InvalidBearerTokenException("로그인 정보가 올바르지 않습니다.");
        }
        AuthSession session = sessions.findById(sessionId).orElse(null);
        if (session == null || !session.isActive(clock.instant())
                || !session.getUser().getId().toString().equals(jwt.getSubject())
                || !session.getUser().isLoginAllowed()) {
            throw new InvalidBearerTokenException("로그인이 만료되었거나 종료되었습니다.");
        }
    }

    private TokenResponse issuePair(AuthSession session) {
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String refresh = session.getId() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        refreshTokens.save(new RefreshToken(hash(refresh), session, clock.instant()));
        var access = jwtService.issue(session.getUser(), session.getId());
        return new TokenResponse(access.tokenType(), access.accessToken(), access.expiresIn(), access.user(),
                refresh, Math.max(0, Duration.between(clock.instant(), session.getExpiresAt()).getSeconds()));
    }

    private RefreshToken verifiedToken(String rawToken, AuthSession session) {
        RefreshToken token = refreshTokens.findById(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
        if (!token.getSession().getId().equals(session.getId())) throw new InvalidRefreshTokenException();
        return token;
    }

    private String sessionId(String token) {
        if (token == null || token.length() > 256) throw new InvalidRefreshTokenException();
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || parts[0].length() != 36 || !parts[1].matches("[A-Za-z0-9_-]{43}")) {
            throw new InvalidRefreshTokenException();
        }
        try {
            return UUID.fromString(parts[0]).toString();
        } catch (IllegalArgumentException exception) {
            throw new InvalidRefreshTokenException();
        }
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
