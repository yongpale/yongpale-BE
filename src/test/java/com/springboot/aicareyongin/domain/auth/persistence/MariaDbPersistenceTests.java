package com.springboot.aicareyongin.domain.auth.persistence;

import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

// 전용 임시 DB에서만 실행한다. ddl-auto=create-drop이므로 업무 DB URL을 지정하지 않는다.
@EnabledIfEnvironmentVariable(named = "MARIADB_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
        "app.data.regions.enabled=false",
        "spring.datasource.url=${MARIADB_TEST_URL}",
        "spring.datasource.username=root",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
        "spring.jpa.properties.hibernate.timezone.default_storage=NORMALIZE_UTC"
})
@Transactional
class MariaDbPersistenceTests {
    @Autowired EntityManager entityManager;
    @Autowired UserRepository users;

    @Test
    void persistsKakaoMemberWithoutPasswordAndPreservesTimestampInstant() {
        OffsetDateTime loginAt = OffsetDateTime.parse("2026-11-06T16:00:00+09:00");
        User user = User.builder().kakaoId(123L).nickname("카카오회원").build();
        user.recordLogin(loginAt);
        user = users.saveAndFlush(user);
        Long id = user.getId();
        entityManager.clear();
        User stored = users.findById(id).orElseThrow();
        assertThat(stored.getKakaoId()).isEqualTo(123L);
        assertThat(stored.getPasswordHash()).isNull();
        assertThat(stored.getTermsAgreedAt()).isNull();
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getLastLoginAt().toInstant()).isEqualTo(loginAt.toInstant());
    }

    @Test
    void persistsJsonDocumentAndRegionIdArray() {
        Policy policy = Policy.builder().title("돌봄 정책").category(PolicyCategory.CARE)
                .requiredDocuments("[\"주민등록등본\"]").targetRegionIds(new Long[]{1L, 2L})
                .sourceUrl("https://example.com/policy").asOfDate(LocalDate.of(2026, 11, 6)).build();
        entityManager.persist(policy);
        entityManager.flush();
        Long id = policy.getId();
        entityManager.clear();
        Policy stored = entityManager.find(Policy.class, id);
        assertThat(stored.getRequiredDocuments()).isEqualTo("[\"주민등록등본\"]");
        assertThat(stored.getTargetRegionIds()).containsExactly(1L, 2L);
    }
}
