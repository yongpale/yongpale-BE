package com.springboot.aicareyongin;

import com.springboot.aicareyongin.domain.facility.loader.FacilityCsvLoader;
import com.springboot.aicareyongin.domain.auth.service.TokenSessionService;
import com.springboot.aicareyongin.domain.auth.repository.AuthSessionRepository;
import com.springboot.aicareyongin.domain.auth.repository.RefreshTokenRepository;
import com.springboot.aicareyongin.domain.auth.service.KakaoUserService;
import com.springboot.aicareyongin.domain.auth.security.KakaoPrincipal;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;
import static org.mockito.ArgumentMatchers.any;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.domain.user.repository.UserRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.data.regions.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:auth-test;MODE=MariaDB;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@AutoConfigureMockMvc
class AiCareYongInApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired TokenSessionService tokenSessions;
    @Autowired AuthSessionRepository sessions;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired UserRepository users;
    @MockitoBean KakaoUserService kakaoUsers;
    @MockitoBean FacilityCsvLoader facilityCsvLoader;
    private User member;

    @BeforeEach
    void setUp() {
        refreshTokens.deleteAllInBatch();
        sessions.deleteAllInBatch();
        users.deleteAllInBatch();
        member = users.saveAndFlush(User.builder().kakaoId(123L).nickname("민재").build());
    }

    @Test
    void unauthenticatedApiReturns401InsteadOfLoginRedirect() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginStartsAtKakaoWithRegisteredCallbackAndState() throws Exception {
        mvc.perform(get("/api/v1/auth/kakao"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/oauth2/authorization/kakao"));
        mvc.perform(get("/oauth2/authorization/kakao"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://kauth.kakao.com/oauth/authorize?")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("state=")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("callback-page")));
    }

    @Test
    void callbackWithoutMatchingStateIsRejected() throws Exception {
        mvc.perform(get("/api/v1/auth/kakao/callback-page").param("code", "fake-code").param("state", "fake-state"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("KAKAO_LOGIN_FAILED"));
    }

    @Test
    void validBearerTokenCanReadOwnProfile() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(member.getId()))
                .andExpect(jsonPath("$.termsAgreementRequired").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedMemberCannotReadProfileEvenWithValidToken() throws Exception {
        String token = bearer();
        users.saveAndFlush(User.builder().id(member.getId()).kakaoId(123L)
                .nickname("탈퇴회원").deletedAt(OffsetDateTime.now()).build());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingRequiredAgreementIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/terms").header("Authorization", bearer())
                        .contentType("application/json").content("{\"termsAgreed\":true,\"privacyAgreed\":false}"))
                .andExpect(status().isBadRequest());
        assertThat(member.getTermsAgreedAt()).isNull();
    }

    @Test
    void recordsExplicitConsentAndKeepsOriginalAgreementTime() throws Exception {
        String token = bearer();
        String body = "{\"termsAgreed\":true,\"privacyAgreed\":true}";
        mvc.perform(post("/api/v1/auth/terms").header("Authorization", token)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.termsAgreementRequired").value(false));
        member = users.findById(member.getId()).orElseThrow();
        OffsetDateTime first = member.getTermsAgreedAt();
        assertThat(first).isNotNull();
        assertThat(member.getPrivacyAgreedAt()).isNotNull();
        mvc.perform(post("/api/v1/auth/terms").header("Authorization", token)
                .contentType("application/json").content(body)).andExpect(status().isOk());
        assertThat(users.findById(member.getId()).orElseThrow().getTermsAgreedAt()).isEqualTo(first);
    }

    @Test
    void successfulCallbackIssuesJwtAndDiscardsTemporarySession() throws Exception {
        when(kakaoUsers.loadUser(any())).thenReturn(new KakaoPrincipal(member.getId()));
        var start = mvc.perform(get("/oauth2/authorization/kakao")).andReturn();
        String location = start.getResponse().getHeader("Location");
        String state = UriUtils.decode(UriComponentsBuilder.fromUriString(location).build()
                .getQueryParams().getFirst("state"), StandardCharsets.UTF_8);
        MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/v1/auth/kakao/callback-page").session(session)
                        .param("code", "test-authorization-code").param("state", state))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.user.id").value(member.getId()));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class OAuthTestConfig {
        @Bean
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> testTokenClient() {
            return request -> OAuth2AccessTokenResponse.withToken("mock-kakao-token")
                    .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(3600).build();
        }
    }

    private String bearer() {
        return "Bearer " + tokenSessions.login(member.getId()).accessToken();
    }
}
