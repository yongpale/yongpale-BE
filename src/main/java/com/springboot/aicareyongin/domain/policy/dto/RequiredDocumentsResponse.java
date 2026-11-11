package com.springboot.aicareyongin.domain.policy.dto;

import com.springboot.aicareyongin.domain.policy.entity.Policy;
import java.util.List;

/**
 * 필요 서류.
 *
 * @param documents         서류 체크리스트. 서류마다 발급처와 발급 링크
 * @param requiredDocuments 정책 원문의 구비서류 목록 (기존 필드, 그대로 유지)
 */
public record RequiredDocumentsResponse(
        Long policyId,
        String title,
        List<DocumentItem> documents,
        List<String> requiredDocuments) {

    public static RequiredDocumentsResponse of(Policy policy, List<DocumentItem> documents, List<String> requiredDocuments) {
        return new RequiredDocumentsResponse(policy.getId(), policy.getTitle(), documents, requiredDocuments);
    }

    /**
     * @param code     서류 코드 (예: RESIDENT_REGISTRATION). 정리된 데이터가 없어 원문으로 만든 항목이면 null
     * @param name     서류 이름 (예: 주민등록표 등본)
     * @param note     이 정책에서의 설명 (예: 3개월 이내 발급). 없으면 null
     * @param issuer   발급처 (예: 정부24, 직장 발급, 신청 페이지). 모르면 null
     * @param issueUrl "발급하기"로 이동할 주소. 직접 받아야 하는 서류면 null
     */
    public record DocumentItem(String code, String name, String note, String issuer, String issueUrl) {
    }
}
