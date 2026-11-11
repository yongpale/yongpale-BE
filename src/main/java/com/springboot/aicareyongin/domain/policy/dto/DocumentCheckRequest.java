package com.springboot.aicareyongin.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * @param checked true = 준비 완료로 체크, false = 체크 해제
 */
public record DocumentCheckRequest(@NotNull @Schema(example = "true") Boolean checked) {
}
