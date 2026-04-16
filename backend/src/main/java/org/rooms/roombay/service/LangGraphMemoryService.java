package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "roombay.ai.memory.enabled", havingValue = "true")
public class LangGraphMemoryService implements AiMemoryService {

    private final RestClient.Builder restClientBuilder;
    private final ConcurrentMap<String, List<MemoryTurn>> localFallbackMemory = new ConcurrentHashMap<>();

    @Value("${roombay.ai.memory.provider:langgraph}")
    private String provider;

    @Value("${roombay.ai.memory.langgraph.base-url:http://localhost:8123}")
    private String baseUrl;

    @Value("${roombay.ai.memory.langgraph.api-key:}")
    private String apiKey;

    @Value("${roombay.ai.memory.request-timeout-ms:2000}")
    private int requestTimeoutMs;

    @Override
    public List<MemoryTurn> loadRecentTurns(UUID userId, String threadId, int limit) {
        if (!isLangGraphProvider()) {
            return readLocalFallback(threadKey(userId, threadId), limit);
        }
        if (threadId == null || threadId.isBlank() || limit <= 0) {
            return List.of();
        }

        String key = threadKey(userId, threadId);
        try {
            RestClient client = buildClient();
            Map<?, ?> res = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/memory/threads/{threadKey}")
                            .queryParam("limit", limit)
                            .build(key))
                    .retrieve()
                    .body(Map.class);
            if (res == null) {
                return List.of();
            }
            Object rawTurns = res.get("turns");
            if (!(rawTurns instanceof List<?> turns) || turns.isEmpty()) {
                return List.of();
            }
            return turns.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(this::mapTurn)
                    .filter(t -> t.userMessage() != null && t.assistantAnswer() != null)
                    .toList();
        } catch (Exception ex) {
            log.warn("[AI] LangGraph memory read failed (timeout={}ms): {}", requestTimeoutMs, ex.getMessage());
            return readLocalFallback(key, limit);
        }
    }

    @Override
    public void appendTurn(UUID userId, String threadId, String userMessage, String assistantAnswer) {
        if (!isLangGraphProvider()) {
            appendLocalFallback(threadKey(userId, threadId), userMessage, assistantAnswer);
            return;
        }
        if (threadId == null || threadId.isBlank()) {
            return;
        }
        String key = threadKey(userId, threadId);
        try {
            RestClient client = buildClient();
            client.post()
                    .uri("/memory/threads/{threadKey}/turns", key)
                    .body(Map.of(
                            "userMessage", userMessage,
                            "assistantAnswer", assistantAnswer
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("[AI] LangGraph memory write failed (timeout={}ms): {}", requestTimeoutMs, ex.getMessage());
            appendLocalFallback(key, userMessage, assistantAnswer);
        }
    }

    @Override
    public boolean isEnabled() {
        return isLangGraphProvider();
    }

    private boolean isLangGraphProvider() {
        return "langgraph".equalsIgnoreCase(provider);
    }

    private RestClient buildClient() {
        RestClient.Builder b = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (apiKey != null && !apiKey.isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        return b.build();
    }

    private String threadKey(UUID userId, String threadId) {
        if (threadId == null) {
            return userId + ":";
        }
        return userId + ":" + threadId;
    }

    private MemoryTurn mapTurn(Map<?, ?> row) {
        Object user = row.get("userMessage");
        Object assistant = row.get("assistantAnswer");
        return new MemoryTurn(
                user instanceof String s ? s : null,
                assistant instanceof String s ? s : null
        );
    }

    private List<MemoryTurn> readLocalFallback(String key, int limit) {
        List<MemoryTurn> turns = localFallbackMemory.getOrDefault(key, List.of());
        if (turns.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, turns.size() - limit);
        return List.copyOf(turns.subList(from, turns.size()));
    }

    private void appendLocalFallback(String key, String userMessage, String assistantAnswer) {
        if (key == null || key.isBlank()) {
            return;
        }
        localFallbackMemory.compute(key, (existingKey, existing) -> {
            List<MemoryTurn> base = existing == null ? List.of() : existing;
            List<MemoryTurn> updated = new java.util.ArrayList<>(base);
            updated.add(new MemoryTurn(userMessage, assistantAnswer));
            // Keep a bounded local fallback history to avoid unbounded growth.
            int keep = 20;
            int from = Math.max(0, updated.size() - keep);
            return new java.util.ArrayList<>(updated.subList(from, updated.size()));
        });
    }
}
