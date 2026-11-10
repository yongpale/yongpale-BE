package com.springboot.aicareyongin.domain.auth;

import com.springboot.aicareyongin.domain.auth.config.JwtProperties;
import com.springboot.aicareyongin.domain.auth.dto.TokenResponse;
import com.springboot.aicareyongin.domain.auth.repository.AuthSessionRepository;
import com.springboot.aicareyongin.domain.auth.repository.RefreshTokenRepository;
import com.springboot.aicareyongin.domain.auth.service.JwtService;
import com.springboot.aicareyongin.domain.auth.service.TokenSessionService;
import com.springboot.aicareyongin.domain.facility.loader.FacilityCsvLoader;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:session-test;MODE=MariaDB;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.data.regions.enabled=false"
})
@AutoConfigureMockMvc
class TokenSessionApiTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;
    @Autowired TokenSessionService tokens;
    @Autowired UserRepository users;
    @Autowired AuthSessionRepository sessions;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired JwtDecoder decoder;
    @Autowired JwtEncoder encoder;
    @Autowired JwtProperties jwtProperties;
    @Autowired MutableClock clock;
    @MockitoBean FacilityCsvLoader facilities;
    private User member;
    private TokenResponse login;

    @BeforeEach
    void setUp() {
        clock.now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        refreshTokens.deleteAllInBatch();
        sessions.deleteAllInBatch();
        users.deleteAllInBatch();
        member = users.saveAndFlush(User.builder().kakaoId(100L).nickname("민재").build());
        login = tokens.login(member.getId());
    }

    @Test
    void storesOnlyRefreshTokenHashAndIssuesSessionBoundJwt() {
        assertThat(login.refreshToken()).hasSize(80);
        assertThat(login.refreshExpiresIn()).isEqualTo(Duration.ofDays(14).toSeconds());
        var stored = refreshTokens.findAll().get(0);
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(login.refreshToken());
        var access = decoder.decode(login.accessToken());
        assertThat(access.getClaimAsString("sid")).isEqualTo(login.refreshToken().substring(0, 36));
        assertThat(access.getId()).isNotBlank();
    }

    @Test
    void refreshReturnsNewPairWithoutAccessToken() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(body(login)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.user.id").value(member.getId())).andReturn();
        var refreshed = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(refreshed.get("refreshToken").asString()).isNotEqualTo(login.refreshToken());
        String access = refreshed.get("accessToken").asString();
        assertThat(access).isNotEqualTo(login.accessToken());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
        assertThat(refreshTokens.count()).isEqualTo(2);
    }

    @Test
    void expiredAccessTokenCanBeRefreshedEvenIfSentInAuthorizationHeader() throws Exception {
        String sid = decoder.decode(login.accessToken()).getClaimAsString("sid");
        String expired = new JwtService(encoder, jwtProperties,
                Clock.fixed(clock.now.minus(Duration.ofHours(2)), ZoneOffset.UTC)).issue(member, sid).accessToken();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer " + expired)
                        .contentType("application/json").content(body(login)))
                .andExpect(status().isOk());
    }

    @Test
    void replayOfConsumedRefreshTokenRevokesEntireLoginAndCommitsRevocation() throws Exception {
        String result = mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(body(login)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var next = mapper.readTree(result);
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(body(login)))
                .andExpect(status().isUnauthorized());
        assertThat(sessions.findAll().get(0).getRevokedAt()).isNotNull();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + next.get("accessToken").asString()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("refreshToken", next.get("refreshToken").asString()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutImmediatelyBlocksAccessAndRefreshAndIsIdempotent() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").contentType("application/json").content(body(login)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + login.accessToken()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/regions").header("Authorization", "Bearer " + login.accessToken()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(body(login)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").contentType("application/json").content(body(login)))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutDoesNotEndOtherDevicesLogin() throws Exception {
        TokenResponse other = tokens.login(member.getId());
        mvc.perform(post("/api/v1/auth/logout").contentType("application/json").content(body(login)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(body(other)))
                .andExpect(status().isOk());
    }

    @Test
    void expiredRefreshTokenCannotReviveSession() throws Exception {
        clock.now = clock.now.plus(Duration.ofDays(15));
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(body(login)))
                .andExpect(status().isUnauthorized());
        assertThat(sessions.findAll().get(0).getRevokedAt()).isNotNull();
    }

    @Test
    void forgeryOfRefreshSecretDoesNotRevokeTheRealSession() throws Exception {
        String fake = login.refreshToken().substring(0, 37) + "A".repeat(43);
        String request = mapper.writeValueAsString(Map.of("refreshToken", fake));
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(request))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").contentType("application/json").content(request))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + login.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void invalidTokensAndMissingInputsAreRejected() throws Exception {
        for (String invalid : List.of("not-a-token", "invalid.token")) {
            mvc.perform(post("/api/v1/auth/refresh").contentType("application/json")
                    .content(mapper.writeValueAsString(Map.of("refreshToken", invalid))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("refreshToken", "A".repeat(257)))))
                .andExpect(status().isBadRequest());
        for (String path : List.of("/api/v1/auth/refresh", "/api/v1/auth/logout")) {
            for (String body : List.of("{}", "{\"refreshToken\":null}", "{\"refreshToken\":\" \"}")) {
                mvc.perform(post(path).contentType("application/json").content(body)).andExpect(status().isBadRequest());
            }
        }
    }

    @Test
    void refreshRejectsDisabledUserAndRevokesSession() throws Exception {
        users.saveAndFlush(User.builder().id(member.getId()).kakaoId(100L).nickname("회원").status("SUSPENDED").build());
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(body(login)))
                .andExpect(status().isUnauthorized());
        assertThat(sessions.findAll().get(0).getRevokedAt()).isNotNull();
    }

    @Test
    void accessTokenWithoutSessionClaimIsRejected() throws Exception {
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer(jwtProperties.issuer())
                .subject(member.getId().toString()).issuedAt(clock.now).expiresAt(clock.now.plusSeconds(1800)).build();
        String legacy = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(
                        org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + legacy))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAlsoWorksWithInvalidAuthorizationHeaderAndConsumedRefreshToken() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json").content(body(login)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer expired")
                .contentType("application/json").content(body(login))).andExpect(status().isNoContent());
        assertThat(sessions.findAll().get(0).getRevokedAt()).isNotNull();
    }

    @Test
    void concurrentRefreshDetectsReplayRatherThanIssuingTwoUsablePairs() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> refresh = () -> mvc.perform(post("/api/v1/auth/refresh")
                    .contentType("application/json").content(body(login))).andReturn().getResponse().getStatus();
            var results = executor.invokeAll(List.of(refresh, refresh));
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(200, 401);
            assertThat(sessions.findAll().get(0).getRevokedAt()).isNotNull();
        } finally {
            executor.shutdownNow();
        }
    }

    private String body(TokenResponse response) {
        return mapper.writeValueAsString(Map.of("refreshToken", response.refreshToken()));
    }

    @TestConfiguration
    static class ClockConfig {
        @Bean @Primary
        MutableClock mutableClock() { return new MutableClock(); }
    }

    static class MutableClock extends Clock {
        volatile Instant now = Instant.now();
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }
}
