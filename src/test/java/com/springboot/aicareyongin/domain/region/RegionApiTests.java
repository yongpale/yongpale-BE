package com.springboot.aicareyongin.domain.region;

import com.springboot.aicareyongin.domain.auth.service.TokenSessionService;
import com.springboot.aicareyongin.domain.facility.loader.FacilityCsvLoader;
import com.springboot.aicareyongin.domain.household.entity.HouseholdProfile;
import com.springboot.aicareyongin.domain.household.repository.HouseholdProfileRepository;
import com.springboot.aicareyongin.domain.region.entity.Region;
import com.springboot.aicareyongin.domain.region.loader.RegionCsvLoader;
import com.springboot.aicareyongin.domain.region.repository.RegionRepository;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${REGION_TEST_URL:jdbc:h2:mem:region-test;MODE=MariaDB;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${REGION_TEST_USERNAME:sa}",
        "spring.datasource.password=${REGION_TEST_PASSWORD:}",
        "spring.datasource.driver-class-name=${REGION_TEST_DRIVER:org.h2.Driver}",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "app.data.regions.enabled=true"
})
@AutoConfigureMockMvc
@Transactional
class RegionApiTests {
    @Autowired RegionRepository regions;
    @Autowired UserRepository users;
    @Autowired HouseholdProfileRepository profiles;
    @Autowired RegionCsvLoader loader;
    @Autowired EntityManager entityManager;
    @Autowired MockMvc mvc;
    @Autowired TokenSessionService tokenSessions;
    @MockitoBean FacilityCsvLoader facilities;
    private String token;

    @BeforeEach
    void setUp() {
        User user = users.saveAndFlush(User.builder().kakaoId(9001L).nickname("회원").build());
        token = "Bearer " + tokenSessions.login(user.getId()).accessToken();
    }

    @Test
    void startupLoadsAllRegionsAndDistrictFilter() throws Exception {
        assertThat(regions.count()).isEqualTo(39);
        mvc.perform(get("/api/v1/regions").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(39));
        mvc.perform(get("/api/v1/regions").param("guName", " 기흥구 ").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(15))
                .andExpect(jsonPath("$[0].guName").value("기흥구"))
                .andExpect(jsonPath("$[0].dongName").value("구갈동"));
    }

    @Test
    void unknownDistrictReturnsEmptyListAndJwtIsRequired() throws Exception {
        mvc.perform(get("/api/v1/regions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/regions").param("guName", "없는구").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void repeatedLoadingPreservesIdsPopulationAndExistingHouseholdLinks() throws Exception {
        Region original = regions.findByGuNameAndDongName("기흥구", "구갈동").orElseThrow();
        Long id = original.getId();
        regions.saveAndFlush(Region.builder().id(id).guName(original.getGuName()).dongName(original.getDongName())
                .regionType(original.getRegionType()).centerLat(new BigDecimal("37.2800000"))
                .centerLng(new BigDecimal("127.1100000")).childPopulation(1234)
                .populationBaseDate(LocalDate.of(2026, 10, 1)).build());
        User owner = users.findByKakaoId(9001L).orElseThrow();
        HouseholdProfile profile = profiles.saveAndFlush(HouseholdProfile.builder()
                .user(owner).region(regions.findById(id).orElseThrow()).build());
        Long profileId = profile.getId();
        Region missing = regions.findByGuNameAndDongName("기흥구", "상하동").orElseThrow();
        regions.delete(missing);
        regions.flush();
        assertThat(regions.count()).isEqualTo(38);
        loader.run(new DefaultApplicationArguments());
        loader.run(new DefaultApplicationArguments());
        entityManager.clear();
        assertThat(regions.count()).isEqualTo(39);
        Region saved = regions.findByGuNameAndDongName("기흥구", "구갈동").orElseThrow();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getChildPopulation()).isEqualTo(1234);
        assertThat(saved.getPopulationBaseDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(saved.getCenterLat()).isEqualByComparingTo("37.2800000");
        assertThat(saved.getCenterLng()).isEqualByComparingTo("127.1100000");
        assertThat(profiles.findById(profileId).orElseThrow().getRegion().getId()).isEqualTo(id);
        assertThat(regions.findByGuNameAndDongName("기흥구", "상하동")).isPresent();
    }

    @Test
    void householdRequestForSanghaDongNowSucceeds() throws Exception {
        mvc.perform(post("/api/v1/households").header("Authorization", token)
                .contentType("application/json").content("""
                        {"region":{"guName":"기흥구","dongName":"상하동"},
                         "situations":["DUAL_INCOME"],
                         "children":[{"nickname":"홍길동","grade":"초2"}]}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.region.dongName").value("상하동"))
                .andExpect(jsonPath("$.children[0].grade").value("초2"));
    }
}
