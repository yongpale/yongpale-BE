package com.springboot.aicareyongin.domain.policy.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 필요 서류 목록(발급처·발급 링크)과 정책별 필요 서류. data/required-documents.json을 앱 시작 시 메모리에 읽는다.
 * 데이터가 작고 사람이 정리해 관리하는 기준 데이터라 DB 테이블을 두지 않는다.
 * 정책은 policies.source_url의 svcId(정부24 서비스ID)로 찾는다.
 */
@Slf4j
@Component
public class RequiredDocumentCatalog {

    static final String PATH = "data/required-documents.json";
    private static final Pattern SVC_ID = Pattern.compile("svcId=([^&#]+)");

    private final Map<String, DocumentType> documentsByCode;
    private final Map<String, List<PolicyDocument>> documentsBySvcId;

    @Autowired
    public RequiredDocumentCatalog(JsonMapper jsonMapper) throws IOException {
        this(jsonMapper, new ClassPathResource(PATH).getInputStream());
    }

    RequiredDocumentCatalog(JsonMapper jsonMapper, InputStream source) throws IOException {
        CatalogFile file;
        try (source) {
            file = jsonMapper.readValue(source, CatalogFile.class);
        }
        Map<String, DocumentType> types = new HashMap<>();
        for (DocumentType type : file.documents()) {
            if (types.put(type.code(), type) != null) {
                throw new IllegalStateException("필요 서류 데이터에 중복된 서류 코드가 있습니다: " + type.code());
            }
        }
        Map<String, List<PolicyDocument>> policies = new HashMap<>();
        for (PolicyEntry policy : file.policies()) {
            for (PolicyDocument document : policy.documents()) {
                if (!types.containsKey(document.code())) {
                    throw new IllegalStateException("필요 서류 데이터의 정책 " + policy.svcId()
                            + "에 정의되지 않은 서류 코드가 있습니다: " + document.code());
                }
            }
            if (policies.put(policy.svcId(), List.copyOf(policy.documents())) != null) {
                throw new IllegalStateException("필요 서류 데이터에 중복된 정책이 있습니다: " + policy.svcId());
            }
        }
        this.documentsByCode = Map.copyOf(types);
        this.documentsBySvcId = Map.copyOf(policies);
        log.info("필요 서류 데이터 적재: 서류 {}종, 정책 {}개", types.size(), policies.size());
    }

    /** 정책의 필요 서류. 정리된 데이터가 없는 정책이면 빈 값 */
    public Optional<List<PolicyDocument>> findBySourceUrl(String sourceUrl) {
        return svcId(sourceUrl).map(documentsBySvcId::get);
    }

    public DocumentType documentType(String code) {
        return documentsByCode.get(code);
    }

    static Optional<String> svcId(String sourceUrl) {
        if (sourceUrl == null) {
            return Optional.empty();
        }
        Matcher m = SVC_ID.matcher(sourceUrl);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    /**
     * @param issuer   발급처 (정부24, 직장 발급, 신청 페이지 등)
     * @param issueUrl 발급 페이지. 직접 받아야 하는 서류면 null
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DocumentType(String code, String name, String issuer, String issueUrl) {

        /** 신청서·동의서처럼 정책 신청 페이지에서 작성하는 서류 */
        public boolean isFiledOnApplyPage() {
            return "신청 페이지".equals(issuer);
        }
    }

    /** @param note 이 정책에서의 설명 (예: 3개월 이내 발급, 해당자만) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolicyDocument(String code, String note) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PolicyEntry(String svcId, String title, List<PolicyDocument> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogFile(List<DocumentType> documents, List<PolicyEntry> policies) {
    }
}
