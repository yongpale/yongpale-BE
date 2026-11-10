package com.springboot.aicareyongin.domain.caregap.controller;

import com.springboot.aicareyongin.domain.caregap.dto.CareGapAnalyzeRequest;
import com.springboot.aicareyongin.domain.caregap.dto.CareGapAnalyzeResponse;
import com.springboot.aicareyongin.domain.caregap.service.CareGapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/care-gap")
@Tag(name = "Care Gap", description = "하교 후 돌봄 공백 분석")
public class CareGapController {

    private final CareGapService careGapService;

    @PostMapping("/analyze")
    @Operation(summary = "하교 후 돌봄 공백 분석", description = "하교 시각과 돌봄 필요 시간으로 돌봄 공백을 계산하고, "
            + "공백을 메울 시설 조합과 돌봄 정책을 반환합니다. 모든 항목은 선택이며 본문은 {}로 보내도 됩니다. "
            + "childId를 바꿔 다시 요청하면 다른 자녀로 분석합니다. 가구 정보가 없으면 404입니다. JWT 인증이 필요합니다.")
    public CareGapAnalyzeResponse analyze(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody CareGapAnalyzeRequest request) {
        return careGapService.analyze(userId(jwt), request);
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
