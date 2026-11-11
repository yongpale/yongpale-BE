package com.springboot.aicareyongin.domain.chat.dto;

import com.springboot.aicareyongin.domain.chat.client.AiChatResponse.Item;
import com.springboot.aicareyongin.domain.policy.enums.MatchStatus;
import java.util.List;

/**
 * 챗봇 답변.
 *
 * @param chatSessionId      다음 질문에 보낼 대화 ID
 * @param messageId          이 답변의 ID (도움됨/아님 피드백용)
 * @param profileLabel       "초1 · 기흥구 구갈동 · 맞벌이" (가구 정보가 없으면 null)
 * @param grounded           false면 "근거 없음" 배지 (등록된 자료에서 근거를 찾지 못함)
 * @param intro              답변 첫 문장
 * @param items              번호 목록 (신청처, 제출 서류, 접수 기간 등). 없으면 빈 배열
 * @param sources            근거 출처
 * @param relatedPolicies    관련 정책 바로가기
 * @param relatedFacilities  관련 시설 바로가기
 * @param suggestedQuestions 추천 질문
 */
public record ChatAnswerResponse(
        String chatSessionId,
        Long messageId,
        String profileLabel,
        boolean grounded,
        String intro,
        List<Item> items,
        List<Source> sources,
        List<RelatedPolicy> relatedPolicies,
        List<RelatedFacility> relatedFacilities,
        List<String> suggestedQuestions) {

    /**
     * @param type       POLICY / FACILITY
     * @param policyId   백엔드 정책 ID (type이 POLICY이고 백엔드에 있을 때)
     * @param facilityId 백엔드 시설 ID (type이 FACILITY이고 백엔드에 있을 때)
     * @param title      정책명 또는 시설명
     * @param name       출처 기관·데이터 이름
     * @param updatedAt  정보 기준일
     */
    public record Source(String type, Long policyId, Long facilityId, String title, String name,
                         String url, String updatedAt) {
    }

    /** policyId: 정책 상세 이동용. 백엔드에 없는 정책이면 null */
    public record RelatedPolicy(Long policyId, String name, MatchStatus status) {
    }

    /** facilityId: 시설 상세 이동용. 백엔드에 없는 시설이면 null */
    public record RelatedFacility(Long facilityId, String name) {
    }
}
