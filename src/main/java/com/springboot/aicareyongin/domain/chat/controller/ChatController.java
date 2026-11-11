package com.springboot.aicareyongin.domain.chat.controller;

import com.springboot.aicareyongin.domain.chat.dto.ChatAnswerResponse;
import com.springboot.aicareyongin.domain.chat.dto.ChatFeedbackRequest;
import com.springboot.aicareyongin.domain.chat.dto.ChatMessageRequest;
import com.springboot.aicareyongin.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "AI 돌봄·배움 챗봇")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/messages")
    @Operation(summary = "챗봇에게 질문", description = "등록된 정책·시설 자료를 근거로 답변합니다. 첫 질문은 chatSessionId 없이 보내고, "
            + "응답의 chatSessionId를 다음 질문에 보내면 이전 대화(최근 4턴)를 이어서 답합니다. "
            + "가구 정보가 있으면 가구 조건에 맞춰 답합니다. grounded=false면 근거를 찾지 못한 답변입니다. JWT 인증이 필요합니다.")
    public ChatAnswerResponse ask(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChatMessageRequest request) {
        return chatService.ask(userId(jwt), request);
    }

    @PostMapping("/messages/{messageId}/feedback")
    @Operation(summary = "답변 피드백", description = "\"이 답변이 도움이 되었나요?\"의 도움됨(helpful=true) / 아님(false)을 저장합니다. "
            + "다시 보내면 바뀝니다. 본인 대화의 답변이 아니면 404입니다. JWT 인증이 필요합니다.")
    public ResponseEntity<Void> feedback(@AuthenticationPrincipal Jwt jwt, @PathVariable Long messageId,
                                         @Valid @RequestBody ChatFeedbackRequest request) {
        chatService.giveFeedback(userId(jwt), messageId, request);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) {
        try {
            long id = Long.parseLong(jwt.getSubject());
            if (id > 0) return id;
        } catch (NumberFormatException exception) {
            // JWT 서명은 유효해도 회원 식별자 형식은 별도로 검증한다.
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "올바르지 않은 회원 정보입니다.");
    }
}
