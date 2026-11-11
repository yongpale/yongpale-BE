package com.springboot.aicareyongin.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.springboot.aicareyongin.domain.policy.document.RequiredDocumentCatalog;
import com.springboot.aicareyongin.domain.policy.dto.RequiredDocumentsResponse;
import com.springboot.aicareyongin.domain.policy.dto.RequiredDocumentsResponse.DocumentItem;
import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.repository.PolicyDocumentCheckRepository;
import com.springboot.aicareyongin.domain.policy.repository.PolicyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

class PolicyDocumentCheckTests {

    private static final Long USER_ID = 1L;
    private static final Long CURATED = 28L;   // required-documents.json에 정리된 정책
    private static final Long ORIGINAL = 99L;  // 정리된 데이터가 없는 정책

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final PolicyRepository policies = mock(PolicyRepository.class);
    private final PolicyDocumentCheckRepository checks = mock(PolicyDocumentCheckRepository.class);
    private PolicyService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new PolicyService(policies, jsonMapper, new RequiredDocumentCatalog(jsonMapper), checks);
        Policy curated = policy(CURATED, "https://plus.gov.kr/...?svcSeq=2000189&svcId=B55284200008", "https://online.nkrf.or.kr",
                "[\"주민등록등본\"]");
        Policy original = policy(ORIGINAL, "https://plus.gov.kr/...?svcId=NOT_CURATED", null, "[\"신청서\", \"통장사본\"]");
        when(policies.findById(CURATED)).thenReturn(Optional.of(curated));
        when(policies.findById(ORIGINAL)).thenReturn(Optional.of(original));
        when(checks.findDocumentCodes(anyLong(), anyLong())).thenReturn(List.of());
    }

    @Test
    void 체크한_서류와_진행률을_함께_반환한다() {
        when(checks.findDocumentCodes(USER_ID, CURATED)).thenReturn(List.of("RESIDENT_REGISTRATION", "NK_DEFECTOR"));

        RequiredDocumentsResponse response = service.getRequiredDocuments(USER_ID, CURATED);

        assertThat(response.totalCount()).isEqualTo(response.documents().size()).isGreaterThan(2);
        assertThat(response.checkedCount()).isEqualTo(2);
        assertThat(response.documents()).filteredOn(DocumentItem::checked).extracting(DocumentItem::code)
                .containsExactlyInAnyOrder("RESIDENT_REGISTRATION", "NK_DEFECTOR");
        // 발급 링크는 체크와 관계없이 그대로
        assertThat(response.documents()).filteredOn(d -> d.code().equals("RESIDENT_REGISTRATION"))
                .singleElement().satisfies(d -> assertThat(d.issueUrl()).startsWith("https://plus.gov.kr/"));
    }

    @Test
    void 체크하면_저장하고_해제하면_지운다() {
        service.checkDocument(USER_ID, CURATED, "RESIDENT_REGISTRATION", true);
        verify(checks).insertIfMissing(USER_ID, CURATED, "RESIDENT_REGISTRATION");

        service.checkDocument(USER_ID, CURATED, "RESIDENT_REGISTRATION", false);
        verify(checks).deleteCheck(USER_ID, CURATED, "RESIDENT_REGISTRATION");
    }

    @Test
    void 정책에_없는_서류는_체크할_수_없다() {
        assertThatThrownBy(() -> service.checkDocument(USER_ID, CURATED, "EMPLOYMENT", true))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("필요 서류가 아닙니다");
        verify(checks, never()).insertIfMissing(any(), any(), anyString());
    }

    @Test
    void 정리된_데이터가_없는_정책은_원문_순서_코드로_체크한다() {
        RequiredDocumentsResponse before = service.getRequiredDocuments(USER_ID, ORIGINAL);
        assertThat(before.documents()).extracting(DocumentItem::code).containsExactly("ORIGINAL_1", "ORIGINAL_2");
        assertThat(before.checkedCount()).isZero();

        service.checkDocument(USER_ID, ORIGINAL, "ORIGINAL_2", true);
        verify(checks).insertIfMissing(USER_ID, ORIGINAL, "ORIGINAL_2");
    }

    private Policy policy(Long id, String sourceUrl, String applyUrl, String requiredDocuments) {
        Policy policy = mock(Policy.class);
        when(policy.getId()).thenReturn(id);
        when(policy.getTitle()).thenReturn("정책 " + id);
        when(policy.isActive()).thenReturn(true);
        when(policy.getSourceUrl()).thenReturn(sourceUrl);
        when(policy.getApplyUrl()).thenReturn(applyUrl);
        when(policy.getRequiredDocuments()).thenReturn(requiredDocuments);
        return policy;
    }
}
