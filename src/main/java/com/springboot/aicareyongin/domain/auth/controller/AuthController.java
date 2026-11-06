package com.springboot.aicareyongin.domain.auth.controller;

import com.springboot.aicareyongin.domain.auth.dto.UserResponse;
import com.springboot.aicareyongin.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("/kakao")
    public ResponseEntity<Void> login() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/oauth2/authorization/kakao")
                .cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(authService.getActiveUser(userId(jwt)));
    }

    @PostMapping("/terms")
    public UserResponse agreeTerms(@AuthenticationPrincipal Jwt jwt, @RequestBody TermsRequest request) {
        if (!Boolean.TRUE.equals(request.termsAgreed()) || !Boolean.TRUE.equals(request.privacyAgreed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해야 합니다.");
        }
        return authService.agreeTerms(userId(jwt));
    }

    private Long userId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "올바르지 않은 회원 정보입니다.");
        }
    }

    public record TermsRequest(Boolean termsAgreed, Boolean privacyAgreed) {
    }
}
