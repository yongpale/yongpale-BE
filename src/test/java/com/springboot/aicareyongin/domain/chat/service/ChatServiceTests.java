package com.springboot.aicareyongin.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.springboot.aicareyongin.domain.facility.repository.FacilityRepository;
import com.springboot.aicareyongin.domain.household.entity.HouseholdProfile;
import com.springboot.aicareyongin.domain.household.repository.HouseholdProfileRepository;
import com.springboot.aicareyongin.domain.policy.client.MatchProfileReader;
import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.repository.PolicyRepository;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.global.ai.AiServerClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

class ChatServiceTests {

    private static final Long USER_ID = 1L;
    private static final String SESSION = "session-1";

    private final AuthService authService = mock(AuthService.class);
    private final HouseholdProfileRepository profiles = mock(HouseholdProfileRepository.class);
    private final MatchProfileReader matchProfileReader = mock(MatchProfileReader.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final PolicyRepository policies = mock(PolicyRepository.class);
    private final FacilityRepository facilities = mock(FacilityRepository.class);
    private final AiServerClient ai = mock(AiServerClient.class);

    private final ChatService service = new ChatService(authService, profiles, matchProfileReader, messages,
            policies, facilities, ai, JsonMapper.builder().build());

    private final HouseholdProfile myProfile = profile(10L);

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getNickname()).thenReturn("김용인");
        when(authService.getActiveUser(USER_ID)).thenReturn(user);
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.of(myProfile));
        when(matchProfileReader.readIfPresent(USER_ID)).thenReturn(Optional.empty());

        Policy policy = mock(Policy.class);
        when(policy.getId()).thenReturn(18L);
        when(policy.getTitle()).thenReturn("방과후학교 자유수강권");
        when(policy.getSourceUrl()).thenReturn("https://plus.gov.kr/...?svcSeq=1&svcId=GES000000150");
        when(policies.findAll(any(Specification.class))).thenReturn(List.of(policy));
        when(facilities.findAll()).thenReturn(List.of());
    }

    @Test
    void 첫_질문은_새_대화를_만들고_질문과_답변을_저장한다() {
        when(ai.postLongRunning(eq("/ai/chat"), any(), eq(AiChatResponse.class))).thenReturn(answer());

        ChatAnswerResponse response = service.ask(USER_ID, new ChatMessageRequest(null, " 신청 어디서 해요? "));

        assertThat(response.chatSessionId()).isNotBlank();
        assertThat(response.relatedPolicies()).singleElement()
                .satisfies(p -> assertThat(p.policyId()).isEqualTo(18L));
        verify(messages, never()).findTop8ByChatSessionIdOrderByIdDesc(anyString());

        ArgumentCaptor<AiChatRequest> request = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(ai).postLongRunning(eq("/ai/chat"), request.capture(), eq(AiChatResponse.class));
        assertThat(request.getValue().question()).isEqualTo("신청 어디서 해요?");
        assertThat(request.getValue().history()).isEmpty();
        assertThat(request.getValue().userName()).isEqualTo("김용인");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> saved = ArgumentCaptor.forClass(List.class);
        verify(messages).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ChatMessage::getRole).containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
        assertThat(saved.getValue().get(1).getContent()).isEqualTo("신청은 온라인으로 해요.\n1. 신청처: 온라인 페이지");
        assertThat(saved.getValue().get(1).getAnswerable()).isTrue();
    }

    @Test
    void 이어지는_질문은_이전_대화를_오래된_순으로_보낸다() {
        List<ChatMessage> recent = new ArrayList<>(List.of(
                message(ChatRole.ASSISTANT, "답변1", myProfile),
                message(ChatRole.USER, "질문1", myProfile)));
        when(messages.findTop8ByChatSessionIdOrderByIdDesc(SESSION)).thenReturn(recent);
        when(ai.postLongRunning(eq("/ai/chat"), any(), eq(AiChatResponse.class))).thenReturn(answer());

        ChatAnswerResponse response = service.ask(USER_ID, new ChatMessageRequest(SESSION, "신청 기간 알려줘"));

        assertThat(response.chatSessionId()).isEqualTo(SESSION);
        ArgumentCaptor<AiChatRequest> request = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(ai).postLongRunning(eq("/ai/chat"), request.capture(), eq(AiChatResponse.class));
        assertThat(request.getValue().history()).containsExactly(
                new AiChatRequest.Turn("user", "질문1"), new AiChatRequest.Turn("assistant", "답변1"));
    }

    @Test
    void AI_서버_제한에_맞춰_닉네임과_이전_대화를_잘라서_보낸다() {
        User longName = mock(User.class);
        when(longName.getNickname()).thenReturn("가".repeat(50));
        when(authService.getActiveUser(USER_ID)).thenReturn(longName);
        List<ChatMessage> recent = new ArrayList<>(List.of(message(ChatRole.ASSISTANT, "답".repeat(3000), myProfile)));
        when(messages.findTop8ByChatSessionIdOrderByIdDesc(SESSION)).thenReturn(recent);
        when(ai.postLongRunning(eq("/ai/chat"), any(), eq(AiChatResponse.class))).thenReturn(answer());

        service.ask(USER_ID, new ChatMessageRequest(SESSION, "질문"));

        ArgumentCaptor<AiChatRequest> request = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(ai).postLongRunning(eq("/ai/chat"), request.capture(), eq(AiChatResponse.class));
        assertThat(request.getValue().userName()).hasSize(ChatService.MAX_USER_NAME_LENGTH);
        assertThat(request.getValue().history().get(0).text()).hasSize(ChatService.MAX_HISTORY_TEXT_LENGTH);
    }

    @Test
    void 모르는_판정_값은_null로_내려준다() {
        AiChatResponse unknownStatus = new AiChatResponse("gemini", null, true, true, "답변", List.of(), List.of(),
                List.of(new AiChatResponse.PolicyLink("GES000000150", "방과후학교 자유수강권", "maybe")),
                List.of(), List.of());
        when(ai.postLongRunning(eq("/ai/chat"), any(), eq(AiChatResponse.class))).thenReturn(unknownStatus);

        ChatAnswerResponse response = service.ask(USER_ID, new ChatMessageRequest(null, "질문"));

        assertThat(response.relatedPolicies()).singleElement()
                .satisfies(p -> assertThat(p.status()).isNull());
    }

    @Test
    void 다른_가구의_대화는_이어갈_수_없다() {
        ChatMessage othersMessage = message(ChatRole.USER, "질문", profile(99L));
        when(messages.findTop8ByChatSessionIdOrderByIdDesc(SESSION))
                .thenReturn(new ArrayList<>(List.of(othersMessage)));

        assertThatThrownBy(() -> service.ask(USER_ID, new ChatMessageRequest(SESSION, "질문")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("대화를 찾을 수 없습니다");
        verify(ai, never()).postLongRunning(anyString(), any(), any());
    }

    @Test
    void 없는_대화_ID는_404() {
        when(messages.findTop8ByChatSessionIdOrderByIdDesc(SESSION)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> service.ask(USER_ID, new ChatMessageRequest(SESSION, "질문")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("대화를 찾을 수 없습니다");
    }

    @Test
    void 본인_답변에_피드백을_저장한다() {
        ChatMessage answer = message(ChatRole.ASSISTANT, "답변", myProfile);
        when(messages.findById(5L)).thenReturn(Optional.of(answer));

        service.giveFeedback(USER_ID, 5L, new ChatFeedbackRequest(SESSION, false));

        assertThat(answer.getFeedback()).isEqualTo(ChatFeedback.NOT_HELPFUL);
    }

    @Test
    void 다른_대화나_다른_가구의_답변에는_피드백할_수_없다() {
        ChatMessage mine = message(ChatRole.ASSISTANT, "답변", myProfile);
        ChatMessage others = message(ChatRole.ASSISTANT, "답변", profile(99L));
        ChatMessage question = message(ChatRole.USER, "질문", myProfile);
        when(messages.findById(5L)).thenReturn(Optional.of(mine));
        when(messages.findById(6L)).thenReturn(Optional.of(others));
        when(messages.findById(7L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> service.giveFeedback(USER_ID, 5L, new ChatFeedbackRequest("other", true)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.giveFeedback(USER_ID, 6L, new ChatFeedbackRequest(SESSION, true)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.giveFeedback(USER_ID, 7L, new ChatFeedbackRequest(SESSION, true)))
                .isInstanceOf(ResponseStatusException.class);
    }

    private AiChatResponse answer() {
        return new AiChatResponse("gemini", "초1 · 기흥구 구갈동", true, true, "신청은 온라인으로 해요.",
                List.of(new AiChatResponse.Item("신청처", "온라인 페이지")),
                List.of(new AiChatResponse.Source("policy", "GES000000150", "방과후학교 자유수강권", "정부24", null, null)),
                List.of(new AiChatResponse.PolicyLink("GES000000150", "방과후학교 자유수강권", "eligible")),
                List.of(), List.of("신청 기간 알려줘"));
    }

    private ChatMessage message(ChatRole role, String content, HouseholdProfile profile) {
        return ChatMessage.builder().chatSessionId(SESSION).profile(profile).role(role)
                .content(content).citations("[]").build();
    }

    private HouseholdProfile profile(Long id) {
        HouseholdProfile profile = mock(HouseholdProfile.class);
        when(profile.getId()).thenReturn(id);
        return profile;
    }
}
