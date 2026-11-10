package com.springboot.aicareyongin.domain.policy.controller;

import com.springboot.aicareyongin.domain.policy.dto.PolicyDetailResponse;
import com.springboot.aicareyongin.domain.policy.dto.PolicySummaryResponse;
import com.springboot.aicareyongin.domain.policy.dto.RequiredDocumentsResponse;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import com.springboot.aicareyongin.domain.policy.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/policies")
@Tag(name = "Policies", description = "돌봄·교육 지원 정책")
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    @Operation(summary = "정책 목록 조회", description = "모든 필터는 선택입니다. 제목순으로 정렬합니다. JWT 인증이 필요합니다.")
    public List<PolicySummaryResponse> getPolicies(
            @Parameter(description = "정책 분류 (CARE: 돌봄, AFTERSCHOOL: 방과후·학습, CASH: 교육비)")
            @RequestParam(required = false) PolicyCategory category,
            @Parameter(description = "자녀 학년 (1~6). 이 학년이 대상에 포함된 정책만 반환합니다.")
            @RequestParam(required = false) @Min(1) @Max(6) Integer grade) {
        return policyService.getPolicies(category, grade);
    }

    @GetMapping("/{policyId}")
    @Operation(summary = "정책 상세 조회", description = "없거나 비활성인 정책이면 404를 반환합니다. JWT 인증이 필요합니다.")
    public PolicyDetailResponse getPolicy(@PathVariable Long policyId) {
        return policyService.getPolicy(policyId);
    }

    @GetMapping("/{policyId}/required-documents")
    @Operation(summary = "필요 서류 조회", description = "신청에 필요한 서류 목록입니다. 서류 정보가 없는 정책은 빈 배열을 반환합니다. "
            + "없거나 비활성인 정책이면 404를 반환합니다. JWT 인증이 필요합니다.")
    public RequiredDocumentsResponse getRequiredDocuments(@PathVariable Long policyId) {
        return policyService.getRequiredDocuments(policyId);
    }
}
