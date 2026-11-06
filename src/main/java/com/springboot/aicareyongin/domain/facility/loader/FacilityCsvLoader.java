package com.springboot.aicareyongin.domain.facility.loader;

import com.springboot.aicareyongin.domain.facility.dto.FacilityDetail;
import com.springboot.aicareyongin.domain.facility.dto.OperatingHours;
import com.springboot.aicareyongin.domain.facility.entity.Facility;
import com.springboot.aicareyongin.domain.facility.enums.FacilityType;
import com.springboot.aicareyongin.domain.facility.enums.SourceType;
import com.springboot.aicareyongin.domain.facility.repository.FacilityRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 앱 시작 시 facilities 테이블이 비어 있으면 data/facilities.csv를 적재한다.
 * CSV는 yongpale-AI 저장소의 data/processed/facilities.csv를 복사한 것이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FacilityCsvLoader implements ApplicationRunner {

    private static final String CSV_PATH = "data/facilities.csv";

    private final FacilityRepository facilityRepository;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (facilityRepository.count() > 0) {
            return;
        }

        List<Facility> facilities = new ArrayList<>();
        try (Reader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {
            skipBom(reader);
            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
            for (CSVRecord row : format.parse(reader)) {
                facilities.add(toFacility(row));
            }
        }

        facilityRepository.saveAll(facilities);
        log.info("시설 데이터 {}곳 적재 완료", facilities.size());
    }

    private Facility toFacility(CSVRecord row) {
        return Facility.builder()
                .facilityType(FacilityType.CARE_CENTER)
                .subtype(text(row, "type"))
                .name(text(row, "name"))
                .sourceType(SourceType.MANUAL)
                .sourceRefId(text(row, "id"))
                .guName(text(row, "gu"))
                .dongName(text(row, "dong"))
                .address(text(row, "address"))
                .phone(text(row, "phone"))
                .capacity(integer(row, "capacity"))
                .dataBaseDate(LocalDate.parse(text(row, "verified_at")))
                .raw(jsonMapper.writeValueAsString(toDetail(row)))
                .build();
    }

    private FacilityDetail toDetail(CSVRecord row) {
        String programs = text(row, "programs");
        return new FacilityDetail(
                integer(row, "age_min"),
                integer(row, "age_max"),
                Boolean.TRUE.equals(bool(row, "all_ages")),
                hours(row, "semester"),
                hours(row, "vacation"),
                hours(row, "saturday"),
                hours(row, "temp_semester"),
                hours(row, "temp_vacation"),
                text(row, "fee_type"),
                text(row, "fee_unit"),
                integer(row, "fee_amount"),
                bool(row, "meal"),
                bool(row, "snack"),
                bool(row, "pickup"),
                integer(row, "temp_capacity"),
                programs == null ? List.of() : Arrays.asList(programs.split(" / ")),
                text(row, "application_info"),
                text(row, "source")
        );
    }

    private OperatingHours hours(CSVRecord row, String prefix) {
        return OperatingHours.of(text(row, prefix + "_open"), text(row, prefix + "_close"));
    }

    /** 빈 칸은 null */
    private String text(CSVRecord row, String column) {
        String value = row.get(column);
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** pandas가 빈 칸이 있는 숫자 컬럼을 "23.0"처럼 실수로 저장해서 실수로 읽은 뒤 정수로 바꾼다 */
    private Integer integer(CSVRecord row, String column) {
        String value = text(row, column);
        return value == null ? null : (int) Double.parseDouble(value);
    }

    /** "True" / "False" / 빈 칸(정보 없음) */
    private Boolean bool(CSVRecord row, String column) {
        String value = text(row, column);
        return value == null ? null : Boolean.parseBoolean(value);
    }

    /** 파일이 BOM으로 시작하면(utf-8-sig) 첫 글자를 건너뛴다 */
    private void skipBom(Reader reader) throws IOException {
        reader.mark(1);
        if (reader.read() != '﻿') {
            reader.reset();
        }
    }
}
