package com.springboot.aicareyongin.domain.chat.service;

import com.springboot.aicareyongin.domain.auth.service.AuthService;
import com.springboot.aicareyongin.domain.chat.client.AiChatRequest;
import com.springboot.aicareyongin.domain.chat.client.AiChatResponse;
import com.springboot.aicareyongin.domain.chat.dto.ChatAnswerResponse;
import com.springboot.aicareyongin.domain.chat.dto.ChatFeedbackRequest;
import com.springboot.aicareyongin.domain.chat.dto.ChatMessageRequest;
import com.springboot.aicareyongin.domain.chat.entity.ChatMessage;
import com.springboot.aicareyongin.domain.chat.enums.ChatFeedback;
import com.springboot.aicareyongin.domain.chat.enums.ChatRole;
import com.springboot.aicareyongin.domain.chat.repository.ChatMessageRepository;
import com.springboot.aicareyongin.domain.facility.entity.Facility;
import com.springboot.aicareyongin.domain.facility.repository.FacilityRepository;
import com.springboot.aicareyongin.domain.household.entity.HouseholdProfile;
import com.springboot.aicareyongin.domain.household.repository.HouseholdProfileRepository;
import com.springboot.aicareyongin.domain.policy.client.MatchProfile;
import com.springboot.aicareyongin.domain.policy.client.MatchProfileReader;
import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.MatchStatus;
import com.springboot.aicareyongin.domain.policy.repository.PolicyRepository;
import com.springboot.aicareyongin.domain.policy.repository.PolicySpecs;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.global.ai.AiServerClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI 돌봄·배움 챗봇. 답변은 AI 서버(정책·시설 자료 검색 + Gemini)가 만들고, 백엔드는 가구 정보와
 * 같은 대화의 이전 질문·답변을 붙여 보낸 뒤 질문과 답변을 저장한다.
 * AI 호출 중 DB 커넥션을 잡지 않도록 질문 처리에는 트랜잭션을 두지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String CHAT_PATH = "/ai/chat";
    private static final Pattern SVC_ID = Pattern.compile("svcId=([^&#]+)");
    // AI 서버 ChatRequest 제한: user_name 20자, history[].text 2000자. 넘으면 422라 잘라서 보낸다.
    static final int MAX_USER_NAME_LENGTH = 20;
    static final int MAX_HISTORY_TEXT_LENGTH = 2000;

    private final AuthService authService;
    private final HouseholdProfileRepository profileRepository;
    private final MatchProfileReader matchProfileReader;
    private final ChatMessageRepository chatMessageRepository;
    private final PolicyRepository policyRepository;
    private final FacilityRepository facilityRepository;
    private final AiServerClient aiServerClient;
    private final JsonMapper jsonMapper;

    public ChatAnswerResponse ask(Long userId, ChatMessageRequest request) {
        User user = authService.getActiveUser(userId);
        HouseholdProfile profile = profileRepository.findByUserId(userId).orElse(null);
        MatchProfile aiProfile = matchProfileReader.readIfPresent(userId).orElse(null);

        String sessionId = request.chatSessionId();
        List<AiChatRequest.Turn> history = List.of();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        } else {
            history = history(sessionId, userId);
        }

        String question = request.question().strip();
        long startedAt = System.currentTimeMillis();
        // 챗봇은 AI 서버 안에서 검색 임베딩과 답변 생성(Gemini)을 차례로 호출해 다른 API보다 오래 걸린다.
        AiChatResponse ai = aiServerClient.postLongRunning(CHAT_PATH,
                new AiChatRequest(question, aiProfile, history, truncate(user.getNickname(), MAX_USER_NAME_LENGTH)),
                AiChatResponse.class);
        int latencyMs = (int) (System.currentTimeMillis() - startedAt);
        if (ai == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버 응답이 올바르지 않습니다.");
        }

        ChatMessage answer = save(sessionId, user, profile, question, ai, latencyMs);
        return toResponse(sessionId, answer.getId(), ai);
    }

    @Transactional
    public void giveFeedback(Long userId, Long messageId, ChatFeedbackRequest request) {
        authService.getActiveUser(userId);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .filter(m -> m.getRole() == ChatRole.ASSISTANT)
                .filter(m -> m.getChatSessionId().equals(request.chatSessionId()))
                .filter(m -> ownedBy(m, userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "답변을 찾을 수 없습니다."));
        message.giveFeedback(Boolean.TRUE.equals(request.helpful()) ? ChatFeedback.HELPFUL : ChatFeedback.NOT_HELPFUL);
    }

    /**
     * 같은 대화의 최근 4턴(오래된 순). 다른 회원의 대화이면 없는 대화와 똑같이 404로 처리한다.
     */
    private List<AiChatRequest.Turn> history(String sessionId, Long userId) {
        List<ChatMessage> recent = new ArrayList<>(chatMessageRepository.findTop8ByChatSessionIdOrderByIdDesc(sessionId));
        if (recent.isEmpty() || recent.stream().anyMatch(m -> !ownedBy(m, userId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다. chatSessionId 없이 새 대화를 시작해 주세요.");
        }
        Collections.reverse(recent);
        return recent.stream()
                .map(m -> new AiChatRequest.Turn(m.getRole() == ChatRole.USER ? "user" : "assistant",
                        truncate(m.getContent(), MAX_HISTORY_TEXT_LENGTH)))
                .toList();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean ownedBy(ChatMessage message, Long userId) {
        return Objects.equals(message.getUser().getId(), userId);
    }

    private ChatMessage save(String sessionId, User user, HouseholdProfile profile, String question,
                             AiChatResponse ai, int latencyMs) {
        ChatMessage userMessage = ChatMessage.builder()
                .chatSessionId(sessionId)
                .user(user)
                .profile(profile)
                .role(ChatRole.USER)
                .content(question)
                .citations("[]")
                .build();
        ChatMessage answer = ChatMessage.builder()
                .chatSessionId(sessionId)
                .user(user)
                .profile(profile)
                .role(ChatRole.ASSISTANT)
                .content(answerText(ai))
                .answerable(ai.grounded())
                .citations(jsonMapper.writeValueAsString(nullToEmpty(ai.sources())))
                .modelName(ai.model())
                .latencyMs(latencyMs)
                .build();
        chatMessageRepository.saveAll(List.of(userMessage, answer));
        return answer;
    }

    /** 다음 질문의 이전 대화로 쓰도록 첫 문장과 번호 목록을 한 글로 저장한다. */
    private String answerText(AiChatResponse ai) {
        StringBuilder text = new StringBuilder(ai.intro() == null ? "" : ai.intro());
        List<AiChatResponse.Item> items = nullToEmpty(ai.items());
        for (int i = 0; i < items.size(); i++) {
            text.append('\n').append(i + 1).append(". ").append(items.get(i).title()).append(": ").append(items.get(i).text());
        }
        return text.toString();
    }

    private ChatAnswerResponse toResponse(String sessionId, Long messageId, AiChatResponse ai) {
        Map<String, Policy> policies = policyRepository.findAll(PolicySpecs.active()).stream()
                .filter(p -> svcId(p.getSourceUrl()) != null)
                .collect(Collectors.toMap(p -> svcId(p.getSourceUrl()), Function.identity(), (a, b) -> a));
        Map<String, Facility> facilities = facilityRepository.findAll().stream()
                .filter(f -> f.getSourceRefId() != null)
                .collect(Collectors.toMap(Facility::getSourceRefId, Function.identity(), (a, b) -> a));

        List<ChatAnswerResponse.Source> sources = nullToEmpty(ai.sources()).stream()
                .map(s -> {
                    boolean isPolicy = "policy".equals(s.type());
                    Policy policy = isPolicy ? policies.get(s.id()) : null;
                    Facility facility = isPolicy ? null : facilities.get(s.id());
                    return new ChatAnswerResponse.Source(
                            isPolicy ? "POLICY" : "FACILITY",
                            policy == null ? null : policy.getId(),
                            facility == null ? null : facility.getId(),
                            s.title(), s.name(), s.url(), s.updatedAt());
                })
                .toList();
        List<ChatAnswerResponse.RelatedPolicy> relatedPolicies = nullToEmpty(ai.relatedPolicies()).stream()
                .map(p -> {
                    Policy policy = policies.get(p.id());
                    return new ChatAnswerResponse.RelatedPolicy(
                            policy == null ? null : policy.getId(),
                            policy == null ? p.name() : policy.getTitle(),
                            MatchStatus.fromAi(p.status()));
                })
                .toList();
        List<ChatAnswerResponse.RelatedFacility> relatedFacilities = nullToEmpty(ai.relatedFacilities()).stream()
                .map(f -> {
                    Facility facility = facilities.get(f.id());
                    return new ChatAnswerResponse.RelatedFacility(
                            facility == null ? null : facility.getId(),
                            facility == null ? f.name() : facility.getName());
                })
                .toList();

        return new ChatAnswerResponse(sessionId, messageId, ai.profileLabel(), ai.grounded(), ai.intro(),
                nullToEmpty(ai.items()), sources, relatedPolicies, relatedFacilities, nullToEmpty(ai.suggestedQuestions()));
    }

    private String svcId(String sourceUrl) {
        if (sourceUrl == null) {
            return null;
        }
        Matcher m = SVC_ID.matcher(sourceUrl);
        return m.find() ? m.group(1) : null;
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
