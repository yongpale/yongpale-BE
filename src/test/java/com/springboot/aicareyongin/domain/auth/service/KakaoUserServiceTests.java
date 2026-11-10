package com.springboot.aicareyongin.domain.auth.service;

import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KakaoUserServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-06T00:00:00Z"), ZoneOffset.UTC);
    private final KakaoUserService service = new KakaoUserService(users, clock);

    @BeforeEach
    void setUp() {
        when(users.findByKakaoId(any())).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User source = invocation.getArgument(0);
            return User.builder().id(1L).kakaoId(source.getKakaoId()).email(source.getEmail())
                    .nickname(source.getNickname()).build();
        });
    }

    @Test
    void registersWithoutEmailPasswordOrInventedConsent() {
        assertThat(service.registerOrLogin(Map.of("id", 123L)).userId()).isEqualTo(1L);
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User user = captor.getValue();
        assertThat(user.getKakaoId()).isEqualTo(123L);
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getTermsAgreedAt()).isNull();
        assertThat(user.getPrivacyAgreedAt()).isNull();
        assertThat(user.getNickname()).isEqualTo("카카오회원");
    }

    @Test
    void acceptsOnlyVerifiedEmail() {
        service.registerOrLogin(Map.of("id", 123L, "kakao_account", Map.of(
                "email", "test@example.com", "is_email_valid", true, "is_email_verified", true,
                "profile", Map.of("nickname", "민재"))));
        verify(users).save(argThat(user -> "test@example.com".equals(user.getEmail())
                && "민재".equals(user.getNickname())));
    }

    @Test
    void ignoresUnverifiedEmail() {
        service.registerOrLogin(Map.of("id", 123L, "kakao_account", Map.of("email", "test@example.com")));
        verify(users).save(argThat(user -> user.getEmail() == null));
    }

    @Test
    void reusesExistingMemberAndRecordsLoginTime() {
        User user = User.builder().id(5L).kakaoId(123L).nickname("기존회원").build();
        when(users.findByKakaoId(123L)).thenReturn(Optional.of(user));
        assertThat(service.registerOrLogin(Map.of("id", 123L)).userId()).isEqualTo(5L);
        verify(users, never()).save(any());
        assertThat(user.getLastLoginAt().toInstant()).isEqualTo(clock.instant());
    }

    @Test
    void rejectsDeletedOrDisabledMembers() {
        for (User user : new User[]{
                User.builder().id(1L).status("SUSPENDED").build(),
                User.builder().id(1L).deletedAt(java.time.OffsetDateTime.now(clock)).build()}) {
            when(users.findByKakaoId(123L)).thenReturn(Optional.of(user));
            assertThatThrownBy(() -> service.registerOrLogin(Map.of("id", 123L)))
                    .isInstanceOf(OAuth2AuthenticationException.class);
        }
    }

    @Test
    void rejectsMissingKakaoId() {
        assertThatThrownBy(() -> service.registerOrLogin(Map.of()))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verify(users, never()).save(any());
    }
}
