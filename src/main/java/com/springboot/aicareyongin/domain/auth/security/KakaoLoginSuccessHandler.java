package com.springboot.aicareyongin.domain.auth.security;

import com.springboot.aicareyongin.domain.auth.service.TokenSessionService;
import com.springboot.aicareyongin.domain.auth.config.FrontendAuthProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(FrontendAuthProperties.class)
public class KakaoLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final TokenSessionService tokenSessions;
    private final FrontendAuthProperties frontend;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        KakaoPrincipal principal = (KakaoPrincipal) authentication.getPrincipal();
        try {
            var token = tokenSessions.login(principal.userId());
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Referrer-Policy", "no-referrer");
            String redirect = frontend.successRedirectUri()
                    + "#accessToken=" + URLEncoder.encode(token.accessToken(), StandardCharsets.UTF_8)
                    + "&refreshToken=" + URLEncoder.encode(token.refreshToken(), StandardCharsets.UTF_8);
            response.sendRedirect(redirect);
        } finally {
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
            SecurityContextHolder.clearContext();
        }
    }
}
