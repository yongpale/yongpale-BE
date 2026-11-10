package com.springboot.aicareyongin.domain.household.dto;

import com.springboot.aicareyongin.domain.household.enums.ChildGrade;
import com.springboot.aicareyongin.domain.household.enums.HouseholdSituation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record HouseholdRequest(
        @NotNull @Valid RegionRequest region,
        @NotNull Set<@NotNull HouseholdSituation> situations,
        @NotEmpty List<@NotNull @Valid ChildRequest> children,
        @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "돌봄 시작 시간은 HH:mm 형식이어야 합니다.")
        @Schema(type = "string", example = "13:00", nullable = true, description = "돌봄 시작 시간. 종료 시간과 함께 입력")
        String careNeedStartTime,
        @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "돌봄 종료 시간은 HH:mm 형식이어야 합니다.")
        @Schema(type = "string", example = "18:00", nullable = true, description = "돌봄 종료 시간. 시작 시간보다 늦어야 함")
        String careNeedEndTime
) {
    @AssertTrue(message = "돌봄 시작·종료 시간을 함께 입력하고, 종료 시간은 시작 시간보다 늦어야 합니다.")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isCareNeedTimeValid() {
        if (careNeedStartTime == null && careNeedEndTime == null) return true;
        if (careNeedStartTime == null || careNeedEndTime == null) return false;
        String pattern = "([01][0-9]|2[0-3]):[0-5][0-9]";
        return careNeedStartTime.matches(pattern) && careNeedEndTime.matches(pattern)
                && careNeedStartTime.compareTo(careNeedEndTime) < 0;
    }

    public record RegionRequest(
            @NotBlank @Size(max = 20) @Schema(example = "기흥구") String guName,
            @NotBlank @Size(max = 30) @Schema(example = "구갈동") String dongName
    ) {
    }

    public record ChildRequest(
            @NotBlank @Size(max = 30) @Schema(example = "홍길동") String nickname,
            @NotNull @Schema(type = "string", example = "초2",
                    allowableValues = {"유치5세", "유치6세", "유치7세", "초1", "초2", "초3", "초4", "초5", "초6"})
            ChildGrade grade
    ) {
    }
}
