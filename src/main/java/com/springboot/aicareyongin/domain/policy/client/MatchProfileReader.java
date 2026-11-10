package com.springboot.aicareyongin.domain.policy.client;

import com.springboot.aicareyongin.domain.auth.service.AuthService;
import com.springboot.aicareyongin.domain.household.entity.Child;
import com.springboot.aicareyongin.domain.household.entity.HouseholdProfile;
import com.springboot.aicareyongin.domain.household.enums.ChildGrade;
import com.springboot.aicareyongin.domain.household.repository.ChildRepository;
import com.springboot.aicareyongin.domain.household.repository.HouseholdProfileRepository;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 로그인한 회원의 가구 정보로 매칭 프로필을 만든다.
 * 돌봄 필요 시간은 가구 조회 응답에 없어서 엔티티에서 직접 읽는다.
 */
@Component
@RequiredArgsConstructor
public class MatchProfileReader {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final AuthService authService;
    private final HouseholdProfileRepository profileRepository;
    private final ChildRepository childRepository;

    @Transactional(readOnly = true)
    public MatchProfile read(Long userId) {
        authService.getActiveUser(userId);
        HouseholdProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 가구 정보가 없습니다."));
        List<ChildGrade> grades = childRepository.findAllByProfileIdOrderByIdAsc(profile.getId()).stream()
                .map(Child::getGradeOrder)
                .map(ChildGrade::fromGradeOrder)
                .toList();
        return new MatchProfile(
                grades.size(),
                grades,
                profile.isDualIncome(),
                profile.isSingleParent(),
                profile.isMultiChild(),
                careTime(profile.getCareNeedStartTime(), profile.getCareNeedEndTime()));
    }

    private MatchProfile.CareTime careTime(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return null;
        }
        return new MatchProfile.CareTime(start.format(HH_MM), end.format(HH_MM));
    }
}
