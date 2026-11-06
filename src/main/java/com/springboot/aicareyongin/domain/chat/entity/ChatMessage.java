package com.springboot.aicareyongin.domain.chat.entity;

import com.springboot.aicareyongin.domain.chat.enums.ChatRole;
import com.springboot.aicareyongin.domain.household.entity.HouseholdProfile;
import com.springboot.aicareyongin.global.entity.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_messages_session", columnList = "chat_session_id, created_at")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatMessage extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "chat_session_id", nullable = false, length = 64)
    private String chatSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private HouseholdProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private ChatRole role;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "answerable")
    private Boolean answerable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "citations", nullable = false, columnDefinition = "jsonb")
    private String citations;

    @Column(name = "model_name", length = 50)
    private String modelName;

    @Column(name = "latency_ms")
    private Integer latencyMs;
}
