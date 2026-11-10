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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 로그인한 회원의 가구 정보로 AI 서버에 보낼 프로필을 만든다.
 */
@Component
@RequiredArgsConstructor
public class MatchProfileReader {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final AuthService authService;
    private final HouseholdProfileRepository profileRepository;
    private final ChildRepository childRepository;

    /** 가구 정보가 없으면 404 */
    @Transactional(readOnly = true)
    public MatchProfile read(Long userId) {
        return readHousehold(userId).profile();
    }

    /** 가구 정보가 없으면 빈 값. 가구 정보 없이도 쓸 수 있는 기능(정책 요약 등)에서 쓴다. */
    @Transactional(readOnly = true)
    public Optional<MatchProfile> readIfPresent(Long userId) {
        authService.getActiveUser(userId);
        return profileRepository.findByUserId(userId).map(profile -> toHousehold(profile).profile());
    }

    /** 프로필과 자녀 목록(ID 포함). 자녀를 골라 분석하는 기능(돌봄 공백 등)에서 쓴다. 가구 정보가 없으면 404 */
    @Transactional(readOnly = true)
    public AiHousehold readHousehold(Long userId) {
        authService.getActiveUser(userId);
        return profileRepository.findByUserId(userId)
                .map(this::toHousehold)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 가구 정보가 없습니다."));
    }

    private AiHousehold toHousehold(HouseholdProfile profile) {
        List<AiHousehold.ChildInfo> children = childRepository.findAllByProfileIdOrderByIdAsc(profile.getId()).stream()
                .map(this::toChildInfo)
                .toList();
        var region = profile.getRegion();
        MatchProfile matchProfile = new MatchProfile(
                region.getGuName(),
                region.getDongName(),
                children.size(),
                children.stream().map(AiHousehold.ChildInfo::grade).toList(),
                profile.isDualIncome(),
                profile.isSingleParent(),
                profile.isMultiChild(),
                careTime(profile.getCareNeedStartTime(), profile.getCareNeedEndTime()));
        return new AiHousehold(matchProfile, children);
    }

    private AiHousehold.ChildInfo toChildInfo(Child child) {
        return new AiHousehold.ChildInfo(child.getId(), child.getNickname(), ChildGrade.fromGradeOrder(child.getGradeOrder()));
    }

    private MatchProfile.CareTime careTime(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return null;
        }
        return new MatchProfile.CareTime(start.format(HH_MM), end.format(HH_MM));
    }
}
