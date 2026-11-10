package com.springboot.aicareyongin.domain.auth.controller;

import com.springboot.aicareyongin.domain.auth.dto.UserResponse;
import com.springboot.aicareyongin.domain.auth.dto.RefreshTokenRequest;
import com.springboot.aicareyongin.domain.auth.dto.TokenResponse;
import com.springboot.aicareyongin.domain.auth.service.TokenSessionService;
import com.springboot.aicareyongin.domain.auth.dto.MyPageResponse;
import com.springboot.aicareyongin.domain.auth.service.MyPageService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
    private final TokenSessionService tokenSessions;
    private final MyPageService myPageService;

    @GetMapping("/kakao")
    public ResponseEntity<Void> login() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/oauth2/authorization/kakao")
                .cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/me")
    @Operation(summary = "마이페이지 조회", description = "회원·가족 정보와 집계 정보를 반환합니다. 미연동 집계는 0입니다.")
    public MyPageResponse me(@AuthenticationPrincipal Jwt jwt) {
        return myPageService.getMyPage(userId(jwt));
    }

    @PostMapping("/terms")
    public UserResponse agreeTerms(@AuthenticationPrincipal Jwt jwt, @RequestBody TermsRequest request) {
        if (!Boolean.TRUE.equals(request.termsAgreed()) || !Boolean.TRUE.equals(request.privacyAgreed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해야 합니다.");
        }
        return authService.agreeTerms(userId(jwt));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Access Token과 Refresh Token 재발급")
    @SecurityRequirements
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(tokenSessions.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "현재 로그인 로그아웃")
    @SecurityRequirements
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        tokenSessions.logout(request.refreshToken());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
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
