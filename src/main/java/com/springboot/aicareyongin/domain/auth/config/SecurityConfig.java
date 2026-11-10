package com.springboot.aicareyongin.domain.auth.config;

import com.springboot.aicareyongin.domain.auth.security.KakaoLoginSuccessHandler;
import com.springboot.aicareyongin.domain.auth.security.SessionJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import com.springboot.aicareyongin.domain.auth.service.KakaoUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, KakaoUserService kakaoUserService,
                                            KakaoLoginSuccessHandler successHandler,
                                            SessionJwtAuthenticationConverter jwtConverter) throws Exception {
        DefaultBearerTokenResolver bearerTokens = new DefaultBearerTokenResolver();
        http
                // API 인증은 Authorization 헤더의 JWT만 사용한다. OAuth 로그인은 state를 별도 검증한다.
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/kakao", "/api/v1/auth/kakao/callback-page",
                                "/oauth2/authorization/kakao", "/error").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        // Swagger UI와 API 문서. 문서만 공개하고 API 호출에는 여전히 JWT가 필요하다.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .oauth2Login(oauth -> oauth
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/v1/auth/kakao/callback-page"))
                        .userInfoEndpoint(endpoint -> endpoint.userService(kakaoUserService))
                        .successHandler(successHandler)
                        .failureHandler((request, response, exception) -> {
                            if (request.getSession(false) != null) {
                                request.getSession(false).invalidate();
                            }
                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.setHeader("Cache-Control", "no-store");
                            response.getWriter().write("{\"code\":\"KAKAO_LOGIN_FAILED\",\"message\":\"카카오 로그인에 실패했습니다. 다시 시도해 주세요.\"}");
                        }))
                .oauth2ResourceServer(resource -> resource
                        .bearerTokenResolver(request -> {
                            String path = request.getRequestURI().substring(request.getContextPath().length());
                            if ("POST".equals(request.getMethod()) && ("/api/v1/auth/refresh".equals(path)
                                    || "/api/v1/auth/logout".equals(path))) return null;
                            return bearerTokens.resolve(request);
                        })
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }
}
