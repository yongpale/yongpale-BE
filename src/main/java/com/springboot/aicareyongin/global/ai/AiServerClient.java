package com.springboot.aicareyongin.global.ai;

import java.net.SocketTimeoutException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * AI 서버(FastAPI, yongpale-AI) 호출. AI 서버는 외부에 열지 않고 백엔드만 호출한다.
 * 주소는 ai.server.base-url (환경변수 AI_SERVER_URL)로 바꿀 수 있다.
 */
@Slf4j
@Component
public class AiServerClient {

    // 매칭·돌봄 공백은 규칙 기반이라 빠르다. 요약의 맞춤 팁은 Gemini를 한 번 부른다.
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);
    // 챗봇은 AI 서버 안에서 검색 임베딩과 답변 생성(Gemini, 호출당 최대 15초)을 차례로 부른다.
    private static final Duration LONG_READ_TIMEOUT = Duration.ofSeconds(45);

    private final RestClient restClient;
    private final RestClient longRunningClient;

    public AiServerClient(@Value("${ai.server.base-url:${AI_SERVER_URL:http://localhost:8000}}") String baseUrl) {
        this.restClient = client(baseUrl, READ_TIMEOUT);
        this.longRunningClient = client(baseUrl, LONG_READ_TIMEOUT);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return post(restClient, path, body, responseType);
    }

    /** 응답이 오래 걸리는 호출(챗봇)용. 읽기 대기 시간이 더 길다. */
    public <T> T postLongRunning(String path, Object body, Class<T> responseType) {
        return post(longRunningClient, path, body, responseType);
    }

    private <T> T post(RestClient client, String path, Object body, Class<T> responseType) {
        try {
            return client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException e) {
            log.warn("AI 서버 오류 응답 path={} status={} body={}", path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버 처리 중 오류가 발생했습니다.");
        } catch (RestClientException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                // 연결은 됐지만 응답이 늦은 경우. 서버가 꺼진 경우(503)와 구분한다.
                log.warn("AI 서버 응답 시간 초과 path={}", path);
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI 서버 응답이 늦어지고 있습니다. 잠시 후 다시 시도해 주세요.");
            }
            log.warn("AI 서버 호출 실패 path={} message={}", path, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        }
    }

    private static RestClient client(String baseUrl, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
