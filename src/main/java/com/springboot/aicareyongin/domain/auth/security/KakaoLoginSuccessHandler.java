package com.springboot.aicareyongin.domain.auth.security;

import com.springboot.aicareyongin.domain.auth.service.TokenSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class KakaoLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final TokenSessionService tokenSessions;
    private final JsonMapper mapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        KakaoPrincipal principal = (KakaoPrincipal) authentication.getPrincipal();
        try {
            var token = tokenSessions.login(principal.userId());
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            response.getWriter().write(mapper.writeValueAsString(token));
        } finally {
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
            SecurityContextHolder.clearContext();
        }
    }
}
