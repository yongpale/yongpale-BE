package com.springboot.aicareyongin.domain.policy.dto;

import com.springboot.aicareyongin.domain.policy.entity.Policy;
import java.util.List;

/**
 * 필요 서류 체크리스트.
 *
 * @param checkedCount      준비 완료로 체크한 서류 수 ("현재 n개 준비 완료")
 * @param totalCount        전체 서류 수 ("서류 n개를 준비해 주세요")
 * @param documents         서류 체크리스트. 서류마다 발급처, 발급 링크, 체크 여부
 * @param requiredDocuments 정책 원문의 구비서류 목록 (기존 필드, 그대로 유지)
 */
public record RequiredDocumentsResponse(
        Long policyId,
        String title,
        int checkedCount,
        int totalCount,
        List<DocumentItem> documents,
        List<String> requiredDocuments) {

    public static RequiredDocumentsResponse of(Policy policy, List<DocumentItem> documents, List<String> requiredDocuments) {
        int checked = (int) documents.stream().filter(DocumentItem::checked).count();
        return new RequiredDocumentsResponse(policy.getId(), policy.getTitle(), checked, documents.size(),
                documents, requiredDocuments);
    }

    /**
     * @param code     서류 코드. 체크·해제 API에 쓴다 (예: RESIDENT_REGISTRATION, 원문으로 만든 항목은 ORIGINAL_1)
     * @param name     서류 이름 (예: 주민등록표 등본)
     * @param note     이 정책에서의 설명 (예: 3개월 이내 발급). 없으면 null
     * @param issuer   발급처 (예: 정부24, 직장 발급, 신청 페이지). 모르면 null
     * @param issueUrl "발급하기"로 이동할 주소. 직접 받아야 하는 서류면 null. 체크와는 관계없다
     * @param checked  왼쪽 체크박스로 "준비 완료" 표시했는지
     */
    public record DocumentItem(String code, String name, String note, String issuer, String issueUrl, boolean checked) {

        public DocumentItem withChecked(boolean checked) {
            return new DocumentItem(code, name, note, issuer, issueUrl, checked);
        }
    }
}
