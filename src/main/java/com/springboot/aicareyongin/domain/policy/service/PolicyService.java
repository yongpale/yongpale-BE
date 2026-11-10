package com.springboot.aicareyongin.domain.policy.service;

import com.springboot.aicareyongin.domain.policy.dto.PolicyDetailResponse;
import com.springboot.aicareyongin.domain.policy.dto.PolicySummaryResponse;
import com.springboot.aicareyongin.domain.policy.dto.RequiredDocumentsResponse;
import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import com.springboot.aicareyongin.domain.policy.repository.PolicyRepository;
import com.springboot.aicareyongin.domain.policy.repository.PolicySpecs;
import java.util.List;
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

    private final PolicyRepository policyRepository;
    private final JsonMapper jsonMapper;

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

    public RequiredDocumentsResponse getRequiredDocuments(Long policyId) {
        Policy policy = findActivePolicy(policyId);
        return RequiredDocumentsResponse.of(policy, readDocuments(policy));
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
