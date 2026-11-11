package com.springboot.aicareyongin.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param chatSessionId 답변이 속한 대화 (본인 대화인지 확인용)
 * @param helpful       true = 도움됨, false = 아님. 다시 보내면 바뀐다.
 */
public record ChatFeedbackRequest(
        @NotBlank @Size(max = 64) String chatSessionId,
        @NotNull @Schema(example = "true") Boolean helpful) {
}
