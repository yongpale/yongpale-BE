package com.springboot.aicareyongin.global.ai;

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

    private final RestClient restClient;

    public AiServerClient(@Value("${ai.server.base-url:${AI_SERVER_URL:http://localhost:8000}}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException e) {
            log.warn("AI 서버 오류 응답 path={} status={} body={}", path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버 처리 중 오류가 발생했습니다.");
        } catch (RestClientException e) {
            log.warn("AI 서버 호출 실패 path={} message={}", path, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다.");
        }
    }
}
