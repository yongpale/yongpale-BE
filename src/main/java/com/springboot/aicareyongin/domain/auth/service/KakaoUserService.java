package com.springboot.aicareyongin.domain.auth.service;

import com.springboot.aicareyongin.domain.auth.security.KakaoPrincipal;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KakaoUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserRepository users;
    private final Clock clock;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        return registerOrLogin(delegate.loadUser(request).getAttributes());
    }

    // 카카오 회원 번호로만 조회한다. 이메일 일치에 의한 계정 자동 연결은 하지 않는다.
    KakaoPrincipal registerOrLogin(Map<String, Object> attributes) {
        Object rawId = attributes.get("id");
        if (!(rawId instanceof Number number) || number.longValue() <= 0) {
            throw failure("invalid_kakao_user");
        }
        Long kakaoId = number.longValue();
        Map<?, ?> account = attributes.get("kakao_account") instanceof Map<?, ?> value ? value : Map.of();
        Map<?, ?> profile = account.get("profile") instanceof Map<?, ?> value ? value : Map.of();
        String nickname = profile.get("nickname") instanceof String value && !value.isBlank()
                ? value : "카카오회원";
        nickname = nickname.substring(0, Math.min(nickname.length(), 50));
        String email = Boolean.TRUE.equals(account.get("is_email_valid"))
                && Boolean.TRUE.equals(account.get("is_email_verified"))
                && account.get("email") instanceof String value && value.length() <= 255 ? value : null;
        User user = users.findByKakaoId(kakaoId).orElse(null);
        if (user == null) {
            user = users.save(User.builder().kakaoId(kakaoId).nickname(nickname).email(email).build());
        }
        if (!user.isLoginAllowed()) {
            throw failure("account_disabled");
        }
        user.recordLogin(OffsetDateTime.now(clock));
        return new KakaoPrincipal(user.getId());
    }

    private OAuth2AuthenticationException failure(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }
}
