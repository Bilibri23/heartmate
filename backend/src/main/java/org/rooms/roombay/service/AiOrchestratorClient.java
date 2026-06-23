package org.rooms.roombay.service;

import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.AiChatRequest;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Client for the LangGraph orchestrator sidecar.
 *
 * <p>Only instantiated when {@code roombay.ai.orchestrator.enabled=true}; otherwise
 * the bean is absent and {@link AiAssistantService} keeps its built-in pipeline. Any
 * error, timeout, or blank answer returns {@link Optional#empty()} so the caller
 * transparently falls back. The orchestrator may run allowlisted write tools
 * (favorites, applications, visits) scoped to the caller's userId/role; Spring
 * re-applies {@code AiOutputGuard} on whatever it returns.
 */
@Service
@Slf4j
@ConditionalOnProperty(value = "roombay.ai.orchestrator.enabled", havingValue = "true")
public class AiOrchestratorClient {

    private final RestClient restClient;
    private final String internalToken;

    public AiOrchestratorClient(
            @Value("${roombay.ai.orchestrator.base-url:http://localhost:8131}") String baseUrl,
            @Value("${roombay.ai.orchestrator.request-timeout-ms:20000}") int timeoutMs,
            @Value("${roombay.ai.internal.token:}") String internalToken) {
        this.internalToken = internalToken;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000)));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Optional<AiChatResponse> orchestrate(UUID userId, String role, AiChatRequest request,
                                                String threadId, String requestId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("message", request.getMessage());
            body.put("persona", personaForRole(role));
            body.put("threadId", threadId);
            body.put("userId", userId == null ? null : userId.toString());
            body.put("userRole", role == null ? "STUDENT" : role);
            body.put("requestId", requestId);

            AiChatResponse response = restClient.post()
                    .uri("/orchestrate")
                    .header("X-RoomBay-Internal-Token", internalToken)
                    .header("X-RoomBay-Request-Id", requestId == null ? "" : requestId)
                    .body(body)
                    .retrieve()
                    .body(AiChatResponse.class);

            if (response == null || response.getAnswer() == null || response.getAnswer().isBlank()) {
                log.warn("[AI] orchestrator returned empty answer; falling back");
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (Exception ex) {
            log.warn("[AI] orchestrator unavailable, falling back requestId={} reason={}: {}",
                    requestId, ex.getClass().getSimpleName(), ex.getMessage());
            return Optional.empty();
        }
    }

    private static String personaForRole(String role) {
        if (role == null) {
            return "TENANT";
        }
        return switch (role.toUpperCase()) {
            case "ADMIN" -> "ADMIN";
            case "LANDLORD" -> "LANDLORD";
            default -> "TENANT";
        };
    }
}
