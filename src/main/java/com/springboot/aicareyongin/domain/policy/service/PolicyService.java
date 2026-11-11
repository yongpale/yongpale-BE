package com.springboot.aicareyongin.domain.policy.service;

import com.springboot.aicareyongin.domain.policy.dto.PolicyDetailResponse;
import com.springboot.aicareyongin.domain.policy.dto.PolicySummaryResponse;
import com.springboot.aicareyongin.domain.policy.document.RequiredDocumentCatalog;
import com.springboot.aicareyongin.domain.policy.document.RequiredDocumentCatalog.DocumentType;
import com.springboot.aicareyongin.domain.policy.dto.RequiredDocumentsResponse;
import com.springboot.aicareyongin.domain.policy.dto.RequiredDocumentsResponse.DocumentItem;
import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import com.springboot.aicareyongin.domain.policy.repository.PolicyDocumentCheckRepository;
import com.springboot.aicareyongin.domain.policy.repository.PolicyRepository;
import com.springboot.aicareyongin.domain.policy.repository.PolicySpecs;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    static final String ORIGINAL_CODE_PREFIX = "ORIGINAL_";

    private final PolicyRepository policyRepository;
    private final JsonMapper jsonMapper;
    private final RequiredDocumentCatalog documentCatalog;
    private final PolicyDocumentCheckRepository documentCheckRepository;

    public List<PolicySummaryResponse> getPolicies(PolicyCategory category, Integer grade) {
        return policyRepository.findAll(
                        PolicySpecs.active()
                                .and(PolicySpecs.categoryEquals(category))
                                .and(PolicySpecs.targetsGrade(grade)),
                        Sort.by("title"))
                .stream()
                .map(PolicySummaryResponse::from)
                .toList();
    }

    public PolicyDetailResponse getPolicy(Long policyId) {
        Policy policy = findActivePolicy(policyId);
        return PolicyDetailResponse.of(policy, readDocuments(policy));
    }

    public RequiredDocumentsResponse getRequiredDocuments(Long userId, Long policyId) {
        Policy policy = findActivePolicy(policyId);
        return requiredDocuments(userId, policy);
    }

    /**
     * 서류를 "준비 완료"로 체크하거나 해제하고, 바뀐 체크리스트를 반환한다.
     * 이 정책의 체크리스트에 없는 서류 코드면 404. 이미 같은 상태면 그대로 둔다.
     */
    @Transactional
    public RequiredDocumentsResponse checkDocument(Long userId, Long policyId, String documentCode, boolean checked) {
        Policy policy = findActivePolicy(policyId);
        boolean known = documentItems(policy, readDocuments(policy)).stream()
                .anyMatch(item -> item.code().equals(documentCode));
        if (!known) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이 정책의 필요 서류가 아닙니다.");
        }
        if (checked) {
            documentCheckRepository.insertIfMissing(userId, policyId, documentCode);
        } else {
            documentCheckRepository.deleteCheck(userId, policyId, documentCode);
        }
        return requiredDocuments(userId, policy);
    }

    private RequiredDocumentsResponse requiredDocuments(Long userId, Policy policy) {
        List<String> original = readDocuments(policy);
        Set<String> checkedCodes = Set.copyOf(documentCheckRepository.findDocumentCodes(userId, policy.getId()));
        List<DocumentItem> items = documentItems(policy, original).stream()
                .map(item -> item.withChecked(checkedCodes.contains(item.code())))
                .toList();
        return RequiredDocumentsResponse.of(policy, items, original);
    }

    /**
     * 정리된 서류 데이터가 있으면 발급처·발급 링크와 함께 반환한다. 신청서·동의서는 정책의 온라인 신청 주소로 연결한다.
     * 데이터가 없는 정책은 원문 목록을 그대로 항목으로 만들고(발급처·링크 없음), 체크할 수 있도록
     * 원문 순서로 ORIGINAL_1, ORIGINAL_2 … 코드를 붙인다. 체크 여부는 requiredDocuments에서 채운다.
     */
    private List<DocumentItem> documentItems(Policy policy, List<String> original) {
        return documentCatalog.findBySourceUrl(policy.getSourceUrl())
                .map(documents -> documents.stream().map(document -> {
                    DocumentType type = documentCatalog.documentType(document.code());
                    String issueUrl = type.isFiledOnApplyPage() ? policy.getApplyUrl() : type.issueUrl();
                    return new DocumentItem(type.code(), type.name(), document.note(), type.issuer(), issueUrl, false);
                }).toList())
                .orElseGet(() -> IntStream.range(0, original.size())
                        .mapToObj(i -> new DocumentItem(ORIGINAL_CODE_PREFIX + (i + 1), original.get(i), null, null, null, false))
                        .toList());
    }

    private Policy findActivePolicy(Long policyId) {
        return policyRepository.findById(policyId)
                .filter(Policy::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 정책입니다."));
    }

    // required_documents는 문자열 JSON 배열로 저장되어 있다.
    private List<String> readDocuments(Policy policy) {
        if (policy.getRequiredDocuments() == null) {
            return List.of();
        }
        return List.of(jsonMapper.readValue(policy.getRequiredDocuments(), String[].class));
    }
}
