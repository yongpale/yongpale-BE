package com.springboot.aicareyongin.domain.policy.loader;

import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import com.springboot.aicareyongin.domain.policy.repository.PolicyRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * policies 테이블이 비어 있으면 data/policies.json(AI 레포 산출물)을 적재한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data.policies.enabled", havingValue = "true", matchIfMissing = true)
public class PolicyJsonLoader implements ApplicationRunner {

    // AI 데이터의 match.category(한글) → PolicyCategory
    private static final Map<String, PolicyCategory> CATEGORIES = Map.of(
            "돌봄", PolicyCategory.CARE,
            "방과후·학습", PolicyCategory.AFTERSCHOOL,
            "교육비", PolicyCategory.CASH);

    private final PolicyRepository policyRepository;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (policyRepository.count() > 0) {
            return;
        }
        PolicySeed[] seeds;
        try (InputStream in = new ClassPathResource("data/policies.json").getInputStream()) {
            seeds = jsonMapper.readValue(in, PolicySeed[].class);
        }
        List<Policy> policies = Arrays.stream(seeds).map(this::toPolicy).toList();
        policyRepository.saveAll(policies);
        log.info("정책 데이터 {}건 적재 완료", policies.size());
    }

    private Policy toPolicy(PolicySeed seed) {
        PolicySeed.Texts texts = seed.texts();
        return Policy.builder()
                .title(seed.name())
                .category(CATEGORIES.getOrDefault(seed.match().category(), PolicyCategory.OTHER))
                .providerOrg(seed.agency())
                .targetText(texts.target())
                .benefitText(texts.benefits())
                .applyMethodText(texts.howToApply())
                .requiredDocuments(toDocumentsJson(texts.documents()))
                .targetGradeMin(seed.match().gradeMin())
                .targetGradeMax(seed.match().gradeMax())
                .applyUrl(seed.apply().onlineUrl())
                .sourceUrl(seed.source().url())
                .asOfDate(LocalDate.parse(seed.source().updatedAt()))
                .build();
    }

    // 줄바꿈으로 나열된 구비서류 문자열을 JSON 배열로 저장한다.
    private String toDocumentsJson(String documents) {
        List<String> lines = documents == null ? List.of()
                : documents.lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
        return jsonMapper.writeValueAsString(lines);
    }
}
