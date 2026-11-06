package com.springboot.aicareyongin.domain.auth.service;

import com.springboot.aicareyongin.domain.auth.config.JwtProperties;
import com.springboot.aicareyongin.domain.auth.dto.TokenResponse;
import com.springboot.aicareyongin.domain.auth.dto.UserResponse;
import com.springboot.aicareyongin.domain.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenResponse issue(User user) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer()).subject(user.getId().toString())
                .issuedAt(issuedAt).expiresAt(issuedAt.plus(properties.accessTokenTtl()))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse("Bearer", token, properties.accessTokenTtl().toSeconds(), UserResponse.from(user));
    }
}
