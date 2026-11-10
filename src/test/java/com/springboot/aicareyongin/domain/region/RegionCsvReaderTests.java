package com.springboot.aicareyongin.domain.region;

import com.springboot.aicareyongin.domain.region.enums.RegionType;
import com.springboot.aicareyongin.domain.region.loader.RegionCsvReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegionCsvReaderTests {
    private final RegionCsvReader reader = new RegionCsvReader();
    private static final String HEADER = "행정읍면동,행정읍면동명,데이터기준일자\n";

    @Test
    void readsAll39OfficialRegionsIncludingSanghaDong() throws Exception {
        try (var source = new InputStreamReader(new ClassPathResource("data/regions.csv").getInputStream(), StandardCharsets.UTF_8)) {
            var seeds = reader.read(source);
            assertThat(seeds).hasSize(39);
            assertThat(seeds.stream().filter(seed -> seed.guName().equals("기흥구"))).hasSize(15);
            assertThat(seeds.stream().filter(seed -> seed.guName().equals("처인구"))).hasSize(13);
            assertThat(seeds.stream().filter(seed -> seed.guName().equals("수지구"))).hasSize(11);
            assertThat(seeds).contains(new RegionCsvReader.RegionSeed("기흥구", "상하동", RegionType.DONG));
            assertThat(seeds).contains(new RegionCsvReader.RegionSeed("처인구", "양지읍", RegionType.EUP));
            assertThat(seeds).contains(new RegionCsvReader.RegionSeed("처인구", "백암면", RegionType.MYEON));
        }
    }

    @Test
    void handlesBomAndWhitespace() throws Exception {
        var seeds = reader.read(new StringReader("\uFEFF" + HEADER + " 기흥구   상하동 , 상하동 ,2026-09-07\n"));
        assertThat(seeds).containsExactly(new RegionCsvReader.RegionSeed("기흥구", "상하동", RegionType.DONG));
    }

    @Test
    void rejectsUnknownDistrictOrMismatchedNameOrUnknownType() {
        for (String row : new String[]{"없는구 상하동,상하동,2026-09-07", "기흥구 상하동,구갈동,2026-09-07", "기흥구 상하,상하,2026-09-07"}) {
            assertThatThrownBy(() -> reader.read(new StringReader(HEADER + row)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsDuplicateRegions() {
        String row = "기흥구 상하동,상하동,2026-09-07\n";
        assertThatThrownBy(() -> reader.read(new StringReader(HEADER + row + row)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("중복");
    }

    @Test
    void rejectsMissingHeadersEmptyDataAndInvalidDate() {
        for (String content : new String[]{"주소,이름\n기흥구,상하동", HEADER, HEADER + "기흥구 상하동,상하동,not-a-date"}) {
            assertThatThrownBy(() -> reader.read(new StringReader(content))).isInstanceOf(RuntimeException.class);
        }
    }
}
