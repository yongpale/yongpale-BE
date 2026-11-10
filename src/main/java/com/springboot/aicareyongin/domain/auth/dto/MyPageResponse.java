package com.springboot.aicareyongin.domain.auth.dto;

import com.springboot.aicareyongin.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MyPageResponse(
        Long id,
        @Schema(description = "표시 이름. 현재는 회원의 카카오 닉네임", example = "민재") String name,
        String nickname,
        String email,
        boolean termsAgreementRequired,
        @Schema(description = "가구의 거주 지역. 가족 정보 미등록 시 null", example = "기흥구 구갈동", nullable = true)
        String region,
        @Schema(description = "자녀 입력 순서의 학년 목록. 자녀가 없으면 빈 배열", example = "[\"초2\",\"유치7세\"]")
        List<String> childGrades,
        @Schema(description = "저장한 정책 개수. 집계 기능 연동 전에는 0", example = "0") int savedPolicyCount,
        @Schema(description = "신청 일정 개수. 집계 기능 연동 전에는 0", example = "0") int applicationScheduleCount,
        @Schema(description = "맞춤 혜택 개수. 집계 기능 연동 전에는 0", example = "0") int matchedBenefitCount
) {
    public static MyPageResponse from(User user, String region, List<String> childGrades, MyPageCounts counts) {
        return new MyPageResponse(user.getId(), user.getNickname(), user.getNickname(), user.getEmail(),
                user.getTermsAgreedAt() == null || user.getPrivacyAgreedAt() == null,
                region, List.copyOf(childGrades), counts.savedPolicyCount(), counts.applicationScheduleCount(),
                counts.matchedBenefitCount());
    }
}
