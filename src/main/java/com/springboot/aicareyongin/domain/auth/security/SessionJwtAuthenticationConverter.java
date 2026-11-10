package com.springboot.aicareyongin.domain.auth.security;

import com.springboot.aicareyongin.domain.auth.service.TokenSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final TokenSessionService sessions;
    private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        sessions.requireActiveSession(jwt);
        return delegate.convert(jwt);
    }
}
