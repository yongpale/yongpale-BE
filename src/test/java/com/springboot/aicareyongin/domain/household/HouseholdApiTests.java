package com.springboot.aicareyongin.domain.household;

import com.springboot.aicareyongin.domain.auth.service.TokenSessionService;
import com.springboot.aicareyongin.domain.auth.repository.AuthSessionRepository;
import com.springboot.aicareyongin.domain.auth.repository.RefreshTokenRepository;
import com.springboot.aicareyongin.domain.facility.loader.FacilityCsvLoader;
import com.springboot.aicareyongin.domain.household.repository.ChildRepository;
import com.springboot.aicareyongin.domain.household.repository.HouseholdProfileRepository;
import com.springboot.aicareyongin.domain.region.entity.Region;
import com.springboot.aicareyongin.domain.region.enums.RegionType;
import com.springboot.aicareyongin.domain.region.repository.RegionRepository;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.domain.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.data.regions.enabled=false",
        "spring.datasource.url=${HOUSEHOLD_TEST_URL:jdbc:h2:mem:household-test;MODE=MariaDB;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${HOUSEHOLD_TEST_USERNAME:sa}",
        "spring.datasource.password=${HOUSEHOLD_TEST_PASSWORD:}",
        "spring.datasource.driver-class-name=${HOUSEHOLD_TEST_DRIVER:org.h2.Driver}",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@AutoConfigureMockMvc
class HouseholdApiTests {
    @Autowired MockMvc mvc;
    @Autowired AuthSessionRepository sessions;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired TokenSessionService tokenSessions;
    @Autowired JsonMapper mapper;
    @Autowired UserRepository users;
    @Autowired RegionRepository regions;
    @Autowired HouseholdProfileRepository profiles;
    @Autowired ChildRepository children;
    @MockitoBean FacilityCsvLoader facilityCsvLoader;

    private User member;
    private String token;
    private static final String PATH = "/api/v1/households";
    private static final String REQUEST = """
            {
              "region": {"guName": "기흥구", "dongName": "구갈동"},
              "situations": ["DUAL_INCOME", "MULTI_CHILD"],
              "children": [
                {"nickname": "홍길동", "grade": "초2"},
                {"nickname": "홍길순", "grade": "유치7세"}
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        refreshTokens.deleteAllInBatch();
        sessions.deleteAllInBatch();
        children.deleteAllInBatch();
        profiles.deleteAllInBatch();
        users.deleteAllInBatch();
        regions.deleteAllInBatch();
        regions.saveAndFlush(Region.builder().guName("기흥구").dongName("구갈동")
                .regionType(RegionType.DONG).build());
        regions.saveAndFlush(Region.builder().guName("처인구").dongName("포곡읍")
                .regionType(RegionType.EUP).build());
        member = users.saveAndFlush(User.builder().kakaoId(1001L).nickname("회원").build());
        token = bearer(member);
    }

    @Test
    void createsAndReadsProfileWithChildOrderAndGradeMapping() throws Exception {
        mvc.perform(post(PATH).header("Authorization", token).contentType("application/json").content(REQUEST))
                .andExpect(status().isCreated()).andExpect(header().string("Location", PATH))
                .andExpect(jsonPath("$.region.guName").value("기흥구"))
                .andExpect(jsonPath("$.region.dongName").value("구갈동"))
                .andExpect(jsonPath("$.situations.length()").value(2))
                .andExpect(jsonPath("$.situations[0]").value("DUAL_INCOME"))
                .andExpect(jsonPath("$.situations[1]").value("MULTI_CHILD"))
                .andExpect(jsonPath("$.children[0].birthOrder").value(1))
                .andExpect(jsonPath("$.children[0].nickname").value("홍길동"))
                .andExpect(jsonPath("$.children[0].grade").value("초2"))
                .andExpect(jsonPath("$.children[1].birthOrder").value(2));
        mvc.perform(get(PATH).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.children[1].nickname").value("홍길순"));
        var profile = profiles.findByUserId(member.getId()).orElseThrow();
        var stored = children.findAllByProfileIdOrderByIdAsc(profile.getId());
        assertThat(stored).extracting(child -> child.getGradeOrder()).containsExactly((short) 5, (short) 3);
        assertThat(profile.isDualIncome()).isTrue();
        assertThat(profile.isMultiChild()).isTrue();
        assertThat(profile.isSingleParent()).isFalse();
    }

    @Test
    void updatesRegionConditionsAndEntireChildList() throws Exception {
        String created = create();
        long profileId = mapper.readTree(created).get("id").asLong();
        long oldChildId = mapper.readTree(created).get("children").get(0).get("id").asLong();
        String updated = """
                {"region":{"guName":"처인구","dongName":"포곡읍"},
                 "situations":["SINGLE_PARENT"],
                 "children":[{"nickname":"새첫째","grade":"초6"}]}
                """;
        mvc.perform(put(PATH).header("Authorization", token).contentType("application/json").content(updated))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(profileId))
                .andExpect(jsonPath("$.region.regionType").value("EUP"))
                .andExpect(jsonPath("$.situations.length()").value(1))
                .andExpect(jsonPath("$.situations[0]").value("SINGLE_PARENT"))
                .andExpect(jsonPath("$.children.length()").value(1))
                .andExpect(jsonPath("$.children[0].nickname").value("새첫째"))
                .andExpect(jsonPath("$.children[0].birthOrder").value(1));
        assertThat(children.existsById(oldChildId)).isFalse();
        var profile = profiles.findByUserId(member.getId()).orElseThrow();
        assertThat(profile.isDualIncome()).isFalse();
        assertThat(profile.isMultiChild()).isFalse();
        assertThat(profile.isSingleParent()).isTrue();
        mvc.perform(get(PATH).header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.children[0].grade").value("초6"));
    }

    @Test
    void modificationPreservesTheRequestedOrder() throws Exception {
        create();
        String updated = """
                {"region":{"guName":"기흥구","dongName":"구갈동"},
                 "situations":["DUAL_INCOME","MULTI_CHILD"],
                 "children":[{"nickname":"홍길순","grade":"유치7세"},
                             {"nickname":"홍길동","grade":"초2"}]}
                """;
        mvc.perform(put(PATH).header("Authorization", token).contentType("application/json").content(updated))
                .andExpect(status().isOk());
        mvc.perform(get(PATH).header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.children[0].nickname").value("홍길순"))
                .andExpect(jsonPath("$.children[1].nickname").value("홍길동"));
    }

    @Test
    void rejectsDuplicateRegistration() throws Exception {
        create();
        mvc.perform(post(PATH).header("Authorization", token).contentType("application/json").content(REQUEST))
                .andExpect(status().isConflict());
        assertThat(profiles.count()).isEqualTo(1);
        assertThat(children.count()).isEqualTo(2);
    }

    @Test
    void concurrentRegistrationCreatesExactlyOneHousehold() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> register = () -> mvc.perform(post(PATH).header("Authorization", token)
                    .contentType("application/json").content(REQUEST)).andReturn().getResponse().getStatus();
            var results = executor.invokeAll(List.of(register, register));
            assertThat(List.of(results.get(0).get(), results.get(1).get()))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(profiles.count()).isEqualTo(1);
            assertThat(children.count()).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void doesNotReadOrModifyAnotherMembersHousehold() throws Exception {
        create();
        User other = users.saveAndFlush(User.builder().kakaoId(2002L).nickname("다른회원").build());
        String otherToken = bearer(other);
        mvc.perform(get(PATH).header("Authorization", otherToken)).andExpect(status().isNotFound());
        mvc.perform(put(PATH).header("Authorization", otherToken).contentType("application/json").content(REQUEST))
                .andExpect(status().isNotFound());
        assertThat(profiles.findByUserId(other.getId())).isEmpty();
        mvc.perform(get(PATH).header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.children[0].nickname").value("홍길동"));
    }

    @Test
    void rejectsAnonymousOrInvalidJwtForEveryMethod() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).contentType("application/json").content(REQUEST)).andExpect(status().isUnauthorized());
        mvc.perform(put(PATH).contentType("application/json").content(REQUEST)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDeletedMembers() throws Exception {
        users.saveAndFlush(User.builder().id(member.getId()).kakaoId(1001L).nickname("탈퇴회원")
                .deletedAt(OffsetDateTime.now()).build());
        mvc.perform(get(PATH).header("Authorization", token)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).header("Authorization", token).contentType("application/json").content(REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMissingRequiredFieldsNullChildrenAndUnknownGradeOrSituation() throws Exception {
        var tree = mapper.readTree(REQUEST);
        List<String> invalid = List.of(
                REQUEST.replace("\"초2\"", "\"초7\""),
                REQUEST.replace("\"DUAL_INCOME\"", "\"UNKNOWN\""),
                REQUEST.replace("\"홍길동\"", "\"   \""),
                REQUEST.replace("\"홍길동\"", "\"" + "가".repeat(31) + "\""),
                "{\"region\":null,\"situations\":[],\"children\":[]}",
                "{\"region\":" + tree.get("region") + ",\"situations\":[],\"children\":[null]}",
                "{\"region\":" + tree.get("region") + ",\"situations\":[null],\"children\":" + tree.get("children") + "}",
                "{\"region\":" + tree.get("region") + ",\"situations\":[],\"children\":[{\"nickname\":\"아이\",\"grade\":null}]}"
        );
        for (String body : invalid) {
            mvc.perform(post(PATH).header("Authorization", token).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(profiles.count()).isZero();
    }

    @Test
    void unknownRegionDuringUpdateLeavesExistingHouseholdUntouched() throws Exception {
        create();
        mvc.perform(put(PATH).header("Authorization", token).contentType("application/json")
                        .content(REQUEST.replace("구갈동", "없는동")))
                .andExpect(status().isBadRequest());
        mvc.perform(get(PATH).header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.region.dongName").value("구갈동"))
                .andExpect(jsonPath("$.children.length()").value(2));
    }

    @Test
    void acceptsNoSelectedSituationAndTrimsNames() throws Exception {
        String request = REQUEST.replace("\"DUAL_INCOME\", \"MULTI_CHILD\"", "")
                .replace("\"기흥구\"", "\" 기흥구 \"")
                .replace("\"구갈동\"", "\" 구갈동 \"")
                .replace("\"홍길동\"", "\" 홍길동 \"");
        mvc.perform(post(PATH).header("Authorization", token).contentType("application/json").content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.situations").isEmpty())
                .andExpect(jsonPath("$.children[0].nickname").value("홍길동"));
    }

    private String create() throws Exception {
        return mvc.perform(post(PATH).header("Authorization", token).contentType("application/json").content(REQUEST))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private String bearer(User user) {
        return "Bearer " + tokenSessions.login(user.getId()).accessToken();
    }
}
