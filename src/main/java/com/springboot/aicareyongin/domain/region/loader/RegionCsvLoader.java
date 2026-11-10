package com.springboot.aicareyongin.domain.region.loader;

import com.springboot.aicareyongin.domain.region.repository.RegionRepository;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data.regions.enabled", havingValue = "true", matchIfMissing = true)
public class RegionCsvLoader implements ApplicationRunner {
    private final RegionRepository regions;
    private final RegionCsvReader csvReader;

    @Override
    @Transactional(rollbackFor = IOException.class)
    public void run(ApplicationArguments args) throws IOException {
        // 전체 파일을 먼저 검증한 뒤 적재한다. 기존 행이 하나 있어도 누락 지역을 채운다.
        try (var source = new InputStreamReader(new ClassPathResource("data/regions.csv").getInputStream(),
                StandardCharsets.UTF_8)) {
            var seeds = csvReader.read(source);
            for (var seed : seeds) {
                regions.insertIfMissing(seed.guName(), seed.dongName(), seed.regionType().name());
            }
            log.info("행정구역 기준 데이터 {}개 반영 완료 (기존 지역 유지)", seeds.size());
        }
    }
}
