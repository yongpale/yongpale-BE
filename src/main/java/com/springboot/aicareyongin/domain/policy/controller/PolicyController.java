package com.springboot.aicareyongin.domain.policy.controller;

import com.springboot.aicareyongin.domain.policy.dto.PolicyAiSummaryResponse;
import com.springboot.aicareyongin.domain.policy.dto.PolicyDetailResponse;
import com.springboot.aicareyongin.domain.policy.dto.PolicyMatchListResponse;
import com.springboot.aicareyongin.domain.policy.dto.PolicyMatchResponse;
import com.springboot.aicareyongin.domain.policy.dto.PolicySummaryResponse;
import com.springboot.aicareyongin.domain.policy.dto.RequiredDocumentsResponse;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import com.springboot.aicareyongin.domain.policy.service.PolicyMatchService;
import com.springboot.aicareyongin.domain.policy.service.PolicyService;
import com.springboot.aicareyongin.domain.policy.service.PolicySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/policies")
@Tag(name = "Policies", description = "돌봄·교육 지원 정책")
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyMatchService policyMatchService;
    private final PolicySummaryService policySummaryService;

    @GetMapping
    @Operation(summary = "정책 목록 조회", description = "모든 필터는 선택입니다. 제목순으로 정렬합니다. JWT 인증이 필요합니다.")
    public List<PolicySummaryResponse> getPolicies(
            @Parameter(description = "정책 분류 (CARE: 돌봄, AFTERSCHOOL: 방과후·학습, CASH: 교육비)")
            @RequestParam(required = false) PolicyCategory category,
            @Parameter(description = "자녀 학년 (1~6). 이 학년이 대상에 포함된 정책만 반환합니다.")
            @RequestParam(required = false) @Min(1) @Max(6) Integer grade) {
        return policyService.getPolicies(category, grade);
    }

    @GetMapping("/matches")
    @Operation(summary = "맞춤 정책 매칭", description = "내 가구 정보(자녀 수·학년, 맞벌이, 한부모, 다자녀, 돌봄 필요 시간)로 "
            + "해당 가능·확인 필요 정책을 적합도 순으로 반환합니다. 가구 정보가 없으면 404입니다. JWT 인증이 필요합니다.")
    public PolicyMatchListResponse getMatches(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "정책 분류 (CARE: 돌봄, AFTERSCHOOL: 방과후·학습, CASH: 교육비)")
            @RequestParam(required = false) PolicyCategory category) {
        return policyMatchService.getMatches(userId(jwt), category);
    }

    @GetMapping("/{policyId}/match")
    @Operation(summary = "정책 단건 매칭", description = "해당 정책에 대한 내 매칭 결과와 이유입니다. 해당 안 됨인 경우도 이유를 반환합니다. "
            + "정책이나 가구 정보가 없으면 404입니다. JWT 인증이 필요합니다.")
    public PolicyMatchResponse getMatch(@AuthenticationPrincipal Jwt jwt, @PathVariable Long policyId) {
        return policyMatchService.getMatch(userId(jwt), policyId);
    }

    @GetMapping("/{policyId}/summary")
    @Operation(summary = "AI 정책 요약", description = "신청 자격·지원 혜택·필요 서류·신청 기간 요약과 가구 맞춤 팁입니다. "
            + "가구 정보가 없으면 맞춤 팁(tip)과 판정(status) 없이 요약만 반환합니다. 정책이 없으면 404입니다. JWT 인증이 필요합니다.")
    public PolicyAiSummaryResponse getSummary(@AuthenticationPrincipal Jwt jwt, @PathVariable Long policyId) {
        return policySummaryService.getSummary(userId(jwt), policyId);
    }

    @GetMapping("/{policyId}")
    @Operation(summary = "정책 상세 조회", description = "없거나 비활성인 정책이면 404를 반환합니다. JWT 인증이 필요합니다.")
    public PolicyDetailResponse getPolicy(@PathVariable Long policyId) {
        return policyService.getPolicy(policyId);
    }

    @GetMapping("/{policyId}/required-documents")
    @Operation(summary = "필요 서류 조회", description = "신청에 필요한 서류 체크리스트입니다. documents의 서류마다 발급처(issuer)와 "
            + "발급 링크(issueUrl, 직접 받아야 하는 서류는 null)를 반환합니다. requiredDocuments는 정책 원문의 구비서류 목록입니다. "
            + "서류 정보가 없는 정책은 빈 배열을 반환합니다. 없거나 비활성인 정책이면 404를 반환합니다. JWT 인증이 필요합니다.")
    public RequiredDocumentsResponse getRequiredDocuments(@PathVariable Long policyId) {
        return policyService.getRequiredDocuments(policyId);
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
