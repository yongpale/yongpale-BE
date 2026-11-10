package com.springboot.aicareyongin.domain.household.dto;

import com.springboot.aicareyongin.domain.household.enums.ChildGrade;
import com.springboot.aicareyongin.domain.household.enums.HouseholdSituation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record HouseholdRequest(
        @NotNull @Valid RegionRequest region,
        @NotNull Set<@NotNull HouseholdSituation> situations,
        @NotEmpty List<@NotNull @Valid ChildRequest> children
) {
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
