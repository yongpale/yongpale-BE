package com.springboot.aicareyongin.domain.chat.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.springboot.aicareyongin.domain.policy.client.MatchProfile;
import java.util.List;

/**
 * AI 서버 POST /ai/chat 요청.
 *
 * @param profile  가구 프로필. 가구 정보가 없으면 null
 * @param history  같은 대화의 이전 질문·답변 (오래된 순, 최근 4턴)
 * @param userName 답변에서 부를 이름 (회원 닉네임)
 */
public record AiChatRequest(
        String question,
        MatchProfile profile,
        List<Turn> history,
        @JsonProperty("user_name") String userName) {

    /** @param role user / assistant */
    public record Turn(String role, String text) {
    }
}
