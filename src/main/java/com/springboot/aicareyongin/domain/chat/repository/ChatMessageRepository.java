package com.springboot.aicareyongin.domain.chat.repository;

import com.springboot.aicareyongin.domain.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** 같은 대화의 최근 메시지 8개(4턴)를 최신순으로 */
    List<ChatMessage> findTop8ByChatSessionIdOrderByIdDesc(String chatSessionId);
}
