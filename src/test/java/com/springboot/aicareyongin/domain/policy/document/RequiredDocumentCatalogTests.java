package com.springboot.aicareyongin.domain.policy.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.springboot.aicareyongin.domain.policy.document.RequiredDocumentCatalog.PolicyDocument;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RequiredDocumentCatalogTests {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void 실제_서류_데이터를_읽는다() throws Exception {
        RequiredDocumentCatalog catalog = new RequiredDocumentCatalog(jsonMapper);

        List<PolicyDocument> documents = catalog.findBySourceUrl(
                "https://plus.gov.kr/portal/benefitV2/benefitTotalSrvcList/benefitSrvcDtl?svcSeq=2000189&bnefType=all&svcId=B55284200008")
                .orElseThrow();

        assertThat(documents).extracting(PolicyDocument::code)
                .contains("RESIDENT_REGISTRATION", "NK_DEFECTOR");
        assertThat(catalog.documentType("RESIDENT_REGISTRATION").issueUrl()).startsWith("https://plus.gov.kr/");
        assertThat(catalog.documentType("APPLICATION_FORM").isFiledOnApplyPage()).isTrue();
    }

    @Test
    void 서류_데이터의_정책은_모두_정책_데이터에_있다() throws Exception {
        JsonNode catalog = read(RequiredDocumentCatalog.PATH);
        JsonNode policies = read("data/policies.json");
        Pattern svcId = Pattern.compile("svcId=([^&#]+)");
        List<String> known = new java.util.ArrayList<>();
        for (JsonNode policy : policies) {
            Matcher m = svcId.matcher(policy.get("source").get("url").asString());
            if (m.find()) known.add(m.group(1));
        }
        for (JsonNode policy : catalog.get("policies")) {
            assertThat(known).contains(policy.get("svcId").asString());
        }
    }

    @Test
    void 정리된_데이터가_없는_정책은_빈_값() throws Exception {
        RequiredDocumentCatalog catalog = new RequiredDocumentCatalog(jsonMapper);

        assertThat(catalog.findBySourceUrl("https://example.com/?svcId=NOT_EXISTS")).isEmpty();
        assertThat(catalog.findBySourceUrl(null)).isEmpty();
    }

    @Test
    void 정의되지_않은_서류_코드가_있으면_시작하지_않는다() {
        String json = """
                {"documents": [{"code": "A", "name": "서류", "issuer": "정부24", "issueUrl": null}],
                 "policies": [{"svcId": "1", "title": "정책", "documents": [{"code": "B", "note": null}]}]}
                """;

        assertThatThrownBy(() -> new RequiredDocumentCatalog(jsonMapper, stream(json)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("B");
    }

    private JsonNode read(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return jsonMapper.readTree(in);
        }
    }

    private InputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
