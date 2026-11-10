package com.springboot.aicareyongin.domain.auth.service;

import com.springboot.aicareyongin.domain.auth.dto.MyPageResponse;
import com.springboot.aicareyongin.domain.household.enums.ChildGrade;
import com.springboot.aicareyongin.domain.household.repository.ChildRepository;
import com.springboot.aicareyongin.domain.household.repository.HouseholdProfileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {
    private final AuthService authService;
    private final HouseholdProfileRepository profiles;
    private final ChildRepository children;
    private final MyPageStatsService stats;

    public MyPageResponse getMyPage(Long userId) {
        var user = authService.getActiveUser(userId);
        var profile = profiles.findByUserId(userId).orElse(null);
        String region = null;
        List<String> childGrades = List.of();
        if (profile != null) {
            var residence = profile.getRegion();
            region = residence.getGuName() + " " + residence.getDongName();
            childGrades = children.findAllByProfileIdOrderByIdAsc(profile.getId()).stream()
                    .map(child -> ChildGrade.fromGradeOrder(child.getGradeOrder()).getLabel()).toList();
        }
        return MyPageResponse.from(user, region, childGrades, stats.getCounts(userId));
    }
}
