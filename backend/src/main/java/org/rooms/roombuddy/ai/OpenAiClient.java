package org.rooms.roombuddy.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Minimal OpenAI HTTP client (embeddings + chat completions).
 * Uses env vars: OPENAI_API_KEY, OPENAI_MODEL, OPENAI_EMBEDDING_MODEL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${OPENAI_API_KEY:}")
    private String apiKey;

    @Value("${OPENAI_BASE_URL:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${OPENAI_MODEL:gpt-4o-mini}")
    private String chatModel;

    @Value("${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}")
    private String embeddingModel;

    public List<Double> embed(String input) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("OPENAI_API_KEY is not configured");
        }

        Map<String, Object> payload = Map.of(
                "model", embeddingModel,
                "input", input
        );

        var client = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<?, ?> res = client.post()
                .uri("/embeddings")
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (res == null || res.get("data") == null) {
            throw new BadRequestException("Embedding request failed");
        }

        List<?> data = (List<?>) res.get("data");
        if (data.isEmpty()) throw new BadRequestException("Embedding response was empty");
        Map<?, ?> item0 = (Map<?, ?>) data.get(0);
        List<?> embedding = (List<?>) item0.get("embedding");
        if (embedding == null || embedding.isEmpty()) throw new BadRequestException("Embedding missing in response");

        return embedding.stream().map(v -> ((Number) v).doubleValue()).toList();
    }

    public String chat(String system, String user, List<Map<String, Object>> contextChunks, String userContext) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("OPENAI_API_KEY is not configured");
        }

        String contextJson = contextChunks == null || contextChunks.isEmpty()
                ? "[]"
                : contextChunks.toString();

        String userPrompt = user + "\n\n" +
                (userContext != null && !userContext.isBlank()
                        ? ("User_context (private, for personalization; do not expose sensitive info):\n" + userContext + "\n\n")
                        : "") +
                "Retrieved_context_chunks (for grounding, cite by chunkId):\n" +
                contextJson;

        Map<String, Object> payload = Map.of(
                "model", chatModel,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        var client = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<?, ?> res = client.post()
                .uri("/chat/completions")
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (res == null) throw new BadRequestException("Chat request failed");
        List<?> choices = (List<?>) res.get("choices");
        if (choices == null || choices.isEmpty()) throw new BadRequestException("Chat response was empty");
        Map<?, ?> choice0 = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice0.get("message");
        String content = message != null ? (String) message.get("content") : null;
        if (content == null) throw new BadRequestException("Chat response missing content");
        return content.trim();
    }
}

