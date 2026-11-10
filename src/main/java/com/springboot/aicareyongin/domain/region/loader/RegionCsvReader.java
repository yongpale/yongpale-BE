package com.springboot.aicareyongin.domain.region.loader;

import com.springboot.aicareyongin.domain.region.enums.RegionType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Component;

@Component
public class RegionCsvReader {
    private static final Set<String> DISTRICTS = Set.of("기흥구", "처인구", "수지구");

    public List<RegionSeed> read(Reader source) throws IOException {
        BufferedReader reader = new BufferedReader(source);
        reader.mark(1);
        if (reader.read() != '\uFEFF') reader.reset();
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true).build();
        try (var csv = format.parse(reader)) {
            if (!csv.getHeaderNames().containsAll(List.of("행정읍면동", "행정읍면동명", "데이터기준일자"))) {
                throw new IllegalArgumentException("지역 CSV의 필수 컬럼이 없습니다.");
            }
            List<RegionSeed> seeds = new ArrayList<>();
            Set<String> keys = new HashSet<>();
            for (var row : csv) {
                String[] address = row.get("행정읍면동").strip().split("\\s+", 2);
                String dong = row.get("행정읍면동명").strip();
                if (address.length != 2 || !DISTRICTS.contains(address[0]) || !address[1].equals(dong)
                        || dong.isEmpty() || dong.length() > 30) {
                    throw new IllegalArgumentException("지역 CSV 주소가 올바르지 않습니다. 행: " + row.getRecordNumber());
                }
                LocalDate.parse(row.get("데이터기준일자").strip());
                RegionType type = typeOf(dong);
                if (!keys.add(address[0] + "/" + dong)) {
                    throw new IllegalArgumentException("지역 CSV에 중복 지역이 있습니다: " + address[0] + " " + dong);
                }
                seeds.add(new RegionSeed(address[0], dong, type));
            }
            if (seeds.isEmpty()) throw new IllegalArgumentException("지역 CSV에 데이터가 없습니다.");
            return List.copyOf(seeds);
        }
    }

    private RegionType typeOf(String dong) {
        if (dong.endsWith("읍")) return RegionType.EUP;
        if (dong.endsWith("면")) return RegionType.MYEON;
        if (dong.endsWith("동")) return RegionType.DONG;
        throw new IllegalArgumentException("읍·면·동을 구분할 수 없습니다: " + dong);
    }

    public record RegionSeed(String guName, String dongName, RegionType regionType) {
    }
}
