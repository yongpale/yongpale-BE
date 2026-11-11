package com.springboot.aicareyongin.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param chatSessionId 이어서 질문할 대화. 첫 질문이면 생략하고, 응답의 chatSessionId를 다음 질문에 보낸다.
 * @param question      질문 (최대 500자)
 */
public record ChatMessageRequest(
        @Size(max = 64) @Schema(nullable = true, description = "첫 질문이면 생략") String chatSessionId,
        @NotBlank @Size(max = 500)
        @Schema(example = "초등 1학년 방과후 이용권 50만원 지원금은 어디서 신청하고, 서류는 뭐가 필요한가요?")
        String question) {
}
