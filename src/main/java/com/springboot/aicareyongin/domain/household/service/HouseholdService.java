package com.springboot.aicareyongin.domain.household.service;

import com.springboot.aicareyongin.domain.auth.service.AuthService;
import com.springboot.aicareyongin.domain.household.dto.HouseholdRequest;
import com.springboot.aicareyongin.domain.household.dto.HouseholdResponse;
import com.springboot.aicareyongin.domain.household.entity.Child;
import com.springboot.aicareyongin.domain.household.entity.HouseholdProfile;
import com.springboot.aicareyongin.domain.household.enums.HouseholdSituation;
import com.springboot.aicareyongin.domain.household.repository.ChildRepository;
import com.springboot.aicareyongin.domain.household.repository.HouseholdProfileRepository;
import com.springboot.aicareyongin.domain.region.entity.Region;
import com.springboot.aicareyongin.domain.region.repository.RegionRepository;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.domain.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HouseholdService {
    private final AuthService authService;
    private final UserRepository users;
    private final HouseholdProfileRepository profiles;
    private final ChildRepository children;
    private final RegionRepository regions;

    @Transactional
    public HouseholdResponse create(Long userId, HouseholdRequest request) {
        User user = lockActiveUser(userId);
        if (profiles.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 가구입니다. 가구 수정을 이용해 주세요.");
        }
        HouseholdProfile profile = HouseholdProfile.builder().user(user).region(findRegion(request)).build();
        updateConditions(profile, request);
        profile = profiles.save(profile);
        return HouseholdResponse.from(profile, saveChildren(profile, request));
    }

    public HouseholdResponse get(Long userId) {
        authService.getActiveUser(userId);
        HouseholdProfile profile = findProfile(userId);
        return HouseholdResponse.from(profile, children.findAllByProfileIdOrderByIdAsc(profile.getId()));
    }

    @Transactional
    public HouseholdResponse update(Long userId, HouseholdRequest request) {
        lockActiveUser(userId);
        HouseholdProfile profile = findProfile(userId);
        // 지역 검증이 끝난 뒤 기존 자녀를 교체한다. 모든 변경은 한 트랜잭션에서 반영한다.
        profile.updateRegion(findRegion(request));
        updateConditions(profile, request);
        children.deleteAllByProfileId(profile.getId());
        return HouseholdResponse.from(profile, saveChildren(profile, request));
    }

    private User lockActiveUser(Long userId) {
        // 회원 행을 잠가 같은 회원의 동시 등록·수정을 순서대로 처리한다.
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원이 존재하지 않습니다."));
        if (!user.isLoginAllowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이용할 수 없는 회원입니다.");
        }
        return user;
    }

    private HouseholdProfile findProfile(Long userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 가구 정보가 없습니다."));
    }

    private Region findRegion(HouseholdRequest request) {
        return regions.findByGuNameAndDongName(request.region().guName().strip(), request.region().dongName().strip())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "등록되지 않은 거주 지역입니다."));
    }

    private void updateConditions(HouseholdProfile profile, HouseholdRequest request) {
        profile.updateSituations(request.situations().contains(HouseholdSituation.DUAL_INCOME),
                request.situations().contains(HouseholdSituation.SINGLE_PARENT),
                request.situations().contains(HouseholdSituation.MULTI_CHILD));
    }

    private List<Child> saveChildren(HouseholdProfile profile, HouseholdRequest request) {
        List<Child> saved = new ArrayList<>();
        // IDENTITY 식별자를 요청 순서대로 발급받아 조회 시에도 추가 순서를 유지한다.
        for (var child : request.children()) {
            saved.add(children.save(Child.builder().profile(profile).nickname(child.nickname().strip())
                    .gradeOrder(child.grade().getGradeOrder()).build()));
        }
        return saved;
    }
}
