package com.springboot.aicareyongin.domain.auth;

import com.springboot.aicareyongin.domain.auth.dto.MyPageCounts;
import com.springboot.aicareyongin.domain.auth.repository.AuthSessionRepository;
import com.springboot.aicareyongin.domain.auth.repository.RefreshTokenRepository;
import com.springboot.aicareyongin.domain.auth.service.MyPageStatsService;
import com.springboot.aicareyongin.domain.auth.service.TokenSessionService;
import com.springboot.aicareyongin.domain.facility.loader.FacilityCsvLoader;
import com.springboot.aicareyongin.domain.household.entity.Child;
import com.springboot.aicareyongin.domain.household.entity.HouseholdProfile;
import com.springboot.aicareyongin.domain.household.repository.ChildRepository;
import com.springboot.aicareyongin.domain.household.repository.HouseholdProfileRepository;
import com.springboot.aicareyongin.domain.region.entity.Region;
import com.springboot.aicareyongin.domain.region.enums.RegionType;
import com.springboot.aicareyongin.domain.region.repository.RegionRepository;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:my-page-test;MODE=MariaDB;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.data.regions.enabled=false"
})
@AutoConfigureMockMvc
class MyPageApiTests {
    @Autowired MockMvc mvc;
    @Autowired TokenSessionService tokens;
    @Autowired UserRepository users;
    @Autowired HouseholdProfileRepository profiles;
    @Autowired ChildRepository children;
    @Autowired RegionRepository regions;
    @Autowired AuthSessionRepository sessions;
    @Autowired RefreshTokenRepository refreshTokens;
    @MockitoBean FacilityCsvLoader facilities;
    @MockitoSpyBean MyPageStatsService stats;
    private User member;
    private Region region;
    private String bearer;

    @BeforeEach
    void setUp() {
        children.deleteAllInBatch();
        profiles.deleteAllInBatch();
        refreshTokens.deleteAllInBatch();
        sessions.deleteAllInBatch();
        users.deleteAllInBatch();
        regions.deleteAllInBatch();
        member = users.saveAndFlush(User.builder().kakaoId(777L).nickname("민재").email("test@example.com").build());
        region = regions.saveAndFlush(Region.builder().guName("기흥구").dongName("구갈동").regionType(RegionType.DONG).build());
        bearer = "Bearer " + tokens.login(member.getId()).accessToken();
    }

    @Test
    void returnsMyPageWithRegionOrderedChildGradesAndIntegerPlaceholders() throws Exception {
        HouseholdProfile profile = profiles.saveAndFlush(HouseholdProfile.builder().user(member).region(region).build());
        children.saveAndFlush(Child.builder().profile(profile).nickname("첫째").gradeOrder((short) 5).build());
        children.saveAndFlush(Child.builder().profile(profile).nickname("둘째").gradeOrder((short) 3).build());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(member.getId()))
                .andExpect(jsonPath("$.name").value("민재"))
                .andExpect(jsonPath("$.nickname").value("민재"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.termsAgreementRequired").value(true))
                .andExpect(jsonPath("$.region").value("기흥구 구갈동"))
                .andExpect(jsonPath("$.childGrades.length()").value(2))
                .andExpect(jsonPath("$.childGrades[0]").value("초2"))
                .andExpect(jsonPath("$.childGrades[1]").value("유치7세"))
                .andExpect(jsonPath("$.savedPolicyCount").isNumber()).andExpect(jsonPath("$.savedPolicyCount").value(0))
                .andExpect(jsonPath("$.applicationScheduleCount").isNumber()).andExpect(jsonPath("$.applicationScheduleCount").value(0))
                .andExpect(jsonPath("$.matchedBenefitCount").isNumber()).andExpect(jsonPath("$.matchedBenefitCount").value(0))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.kakaoId").doesNotExist());
    }

    @Test
    void memberWithoutHouseholdStillGetsSuccessfulMyPage() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("민재"))
                .andExpect(jsonPath("$.region").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.childGrades.length()").value(0))
                .andExpect(jsonPath("$.savedPolicyCount").value(0));
    }

    @Test
    void doesNotExposeAnotherMembersFamilyInformation() throws Exception {
        User other = users.saveAndFlush(User.builder().kakaoId(888L).nickname("다른회원").build());
        HouseholdProfile profile = profiles.saveAndFlush(HouseholdProfile.builder().user(other).region(region).build());
        children.saveAndFlush(Child.builder().profile(profile).nickname("다른가족").gradeOrder((short) 9).build());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId()))
                .andExpect(jsonPath("$.region").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.childGrades.length()").value(0));
    }

    @Test
    void preservesDuplicateGradesInsteadOfDeduplicatingChildren() throws Exception {
        HouseholdProfile profile = profiles.saveAndFlush(HouseholdProfile.builder().user(member).region(region).build());
        children.saveAndFlush(Child.builder().profile(profile).nickname("첫째").gradeOrder((short) 5).build());
        children.saveAndFlush(Child.builder().profile(profile).nickname("둘째").gradeOrder((short) 5).build());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.childGrades.length()").value(2))
                .andExpect(jsonPath("$.childGrades[0]").value("초2"))
                .andExpect(jsonPath("$.childGrades[1]").value("초2"));
    }

    @Test
    void forwardsFutureSummaryCountsForTheAuthenticatedMember() throws Exception {
        when(stats.getCounts(member.getId())).thenReturn(new MyPageCounts(7, 3, 12));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.savedPolicyCount").value(7))
                .andExpect(jsonPath("$.applicationScheduleCount").value(3))
                .andExpect(jsonPath("$.matchedBenefitCount").value(12));
        verify(stats).getCounts(member.getId());
    }

    @Test
    void requiresAValidLogin() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        assertThat(profiles.count()).isZero();
    }
}
