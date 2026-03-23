package org.rooms.roombuddy.ai;

import lombok.RequiredArgsConstructor;
import org.rooms.roombuddy.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Minimal Ollama HTTP client for local, free dev.
 * Requires: ollama running (default http://localhost:11434).
 *
 * Env vars:
 * - OLLAMA_BASE_URL (default http://localhost:11434)
 * - OLLAMA_CHAT_MODEL (default llama3.1)
 * - OLLAMA_EMBEDDING_MODEL (default nomic-embed-text)
 */
@Component
@RequiredArgsConstructor
public class OllamaClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${OLLAMA_BASE_URL:http://localhost:11434}")
    private String baseUrl;

    @Value("${OLLAMA_CHAT_MODEL:llama3.1}")
    private String chatModel;

    @Value("${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}")
    private String embeddingModel;

    public List<Double> embed(String input) {
        Map<String, Object> payload = Map.of(
                "model", embeddingModel,
                "input", input
        );

        var client = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<?, ?> res = client.post()
                .uri("/api/embed")
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (res == null) throw new BadRequestException("Ollama embedding request failed");
        List<?> embeddings = (List<?>) res.get("embeddings");
        if (embeddings == null || embeddings.isEmpty()) throw new BadRequestException("Ollama embedding response was empty");
        List<?> vec0 = (List<?>) embeddings.get(0);
        if (vec0 == null || vec0.isEmpty()) throw new BadRequestException("Ollama embedding missing in response");
        return vec0.stream().map(v -> ((Number) v).doubleValue()).toList();
    }

    public String chat(String system, String user, String userContext, List<Map<String, Object>> contextChunks) {
        String contextJson = contextChunks == null || contextChunks.isEmpty() ? "[]" : contextChunks.toString();

        String prompt = system + "\n\n" +
                "User_question:\n" + user + "\n\n" +
                (userContext != null && !userContext.isBlank()
                        ? ("User_context (private):\n" + userContext + "\n\n")
                        : "") +
                "Retrieved_context_chunks:\n" + contextJson + "\n\n" +
                "Answer:";

        Map<String, Object> payload = Map.of(
                "model", chatModel,
                "prompt", prompt,
                "stream", false
        );

        var client = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<?, ?> res = client.post()
                .uri("/api/generate")
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (res == null) throw new BadRequestException("Ollama chat request failed");
        String response = (String) res.get("response");
        if (response == null) throw new BadRequestException("Ollama chat response missing 'response'");
        return response.trim();
    }
}

