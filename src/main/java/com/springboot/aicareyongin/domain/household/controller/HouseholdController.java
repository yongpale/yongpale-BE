package com.springboot.aicareyongin.domain.household.controller;

import com.springboot.aicareyongin.domain.household.dto.HouseholdRequest;
import com.springboot.aicareyongin.domain.household.dto.HouseholdResponse;
import com.springboot.aicareyongin.domain.household.service.HouseholdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/households")
@Tag(name = "Households", description = "로그인 회원의 가족 정보")
public class HouseholdController {
    private final HouseholdService service;

    @PostMapping
    @Operation(summary = "내 가족 정보 등록")
    public ResponseEntity<HouseholdResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody HouseholdRequest request) {
        return ResponseEntity.created(URI.create("/api/v1/households"))
                .body(service.create(userId(jwt), request));
    }

    @GetMapping
    @Operation(summary = "내 가족 정보 조회")
    public HouseholdResponse get(@AuthenticationPrincipal Jwt jwt) {
        return service.get(userId(jwt));
    }

    @PutMapping
    @Operation(summary = "내 가족 정보 전체 수정", description = "자녀 목록도 요청 순서대로 전체 교체합니다.")
    public HouseholdResponse update(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody HouseholdRequest request) {
        return service.update(userId(jwt), request);
    }

    private Long userId(Jwt jwt) {
        try {
            long id = Long.parseLong(jwt.getSubject());
            if (id > 0) return id;
        } catch (NumberFormatException exception) {
            // JWT 서명은 유효해도 회원 식별자 형식은 별도로 검증한다.
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "올바르지 않은 회원 정보입니다.");
    }
}
