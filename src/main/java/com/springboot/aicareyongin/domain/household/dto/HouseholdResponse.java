package com.springboot.aicareyongin.domain.household.dto;

import com.springboot.aicareyongin.domain.household.entity.Child;
import com.springboot.aicareyongin.domain.household.entity.HouseholdProfile;
import com.springboot.aicareyongin.domain.household.enums.ChildGrade;
import com.springboot.aicareyongin.domain.household.enums.HouseholdSituation;
import com.springboot.aicareyongin.domain.region.enums.RegionType;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.stream.IntStream;

public record HouseholdResponse(Long id, RegionResponse region,
                                List<HouseholdSituation> situations, List<ChildResponse> children,
                                @Schema(example = "13:00", nullable = true) String careNeedStartTime,
                                @Schema(example = "18:00", nullable = true) String careNeedEndTime,
                                @Schema(description = "필요 돌봄 시간. 미설정이면 null", example = "13:00-18:00", nullable = true)
                                String careNeedTime) {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static HouseholdResponse from(HouseholdProfile profile, List<Child> children) {
        var region = profile.getRegion();
        String start = profile.getCareNeedStartTime() == null ? null : profile.getCareNeedStartTime().format(TIME_FORMAT);
        String end = profile.getCareNeedEndTime() == null ? null : profile.getCareNeedEndTime().format(TIME_FORMAT);
        List<HouseholdSituation> situations = new ArrayList<>();
        if (profile.isDualIncome()) situations.add(HouseholdSituation.DUAL_INCOME);
        if (profile.isSingleParent()) situations.add(HouseholdSituation.SINGLE_PARENT);
        if (profile.isMultiChild()) situations.add(HouseholdSituation.MULTI_CHILD);
        return new HouseholdResponse(profile.getId(), new RegionResponse(region.getId(),
                region.getGuName(), region.getDongName(), region.getRegionType()), situations,
                IntStream.range(0, children.size()).mapToObj(index -> {
                    Child child = children.get(index);
                    return new ChildResponse(child.getId(), index + 1, child.getNickname(),
                            ChildGrade.fromGradeOrder(child.getGradeOrder()));
                }).toList(), start, end, start == null || end == null ? null : start + "-" + end);
    }

    public record RegionResponse(Long id, String guName, String dongName, RegionType regionType) {
    }

    public record ChildResponse(Long id, int birthOrder, String nickname, ChildGrade grade) {
    }
}
