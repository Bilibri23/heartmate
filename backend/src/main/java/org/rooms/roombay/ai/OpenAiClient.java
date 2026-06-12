package org.rooms.roombay.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Minimal OpenAI HTTP client (embeddings + chat completions).
 * Uses env vars: OPENAI_API_KEY, OPENAI_MODEL, OPENAI_EMBEDDING_MODEL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    private final RestClient.Builder restClientBuilder;

    @Value("${OPENAI_API_KEY:}")
    private String apiKey;

    @Value("${OPENAI_BASE_URL:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${OPENAI_MODEL:gpt-4o-mini}")
    private String chatModel;

    @Value("${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${roombay.ai.debug-synthesis-logging:false}")
    private boolean debugSynthesisLogging;

    @Value("${roombay.ai.request-timeout-ms:85000}")
    private long requestTimeoutMs;

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
        return chat(system, user, contextChunks, userContext, null);
    }

    /**
     * @param modelOverride when non-blank, replaces {@link #chatModel} (e.g. {@code ft:...} fine-tuned id).
     */
    public String chat(String system, String user, List<Map<String, Object>> contextChunks, String userContext,
                       String modelOverride) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("OPENAI_API_KEY is not configured");
        }

        String m = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : chatModel;

        String userPrompt = buildUserPrompt(user, userContext, contextChunks);

        Map<String, Object> payload = Map.of(
                "model", m,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        if (debugSynthesisLogging) {
            log.info("[AI] OpenAI final prompt model={} system=\n{}\nuser=\n{}", m, system, userPrompt);
        }

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

    public String chatStream(String system, String user, List<Map<String, Object>> contextChunks, String userContext,
                             String modelOverride, Consumer<String> onToken) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("OPENAI_API_KEY is not configured");
        }

        String m = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : chatModel;
        String userPrompt = buildUserPrompt(user, userContext, contextChunks);
        Map<String, Object> payload = Map.of(
                "model", m,
                "temperature", 0.2,
                "stream", true,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String body = JSON.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<java.io.InputStream> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 400) {
                throw new BadRequestException("OpenAI streaming request failed with status " + response.statusCode());
            }

            StringBuilder full = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring("data:".length()).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) continue;
                    var root = JSON.readTree(data);
                    var choices = root.path("choices");
                    if (!choices.isArray() || choices.isEmpty()) continue;
                    String token = choices.get(0).path("delta").path("content").asText("");
                    if (token.isEmpty()) continue;
                    full.append(token);
                    if (onToken != null) {
                        onToken.accept(token);
                    }
                }
            }
            if (full.isEmpty()) {
                throw new BadRequestException("OpenAI streaming response was empty");
            }
            return full.toString().trim();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("OpenAI streaming failed: " + ex.getMessage());
        }
    }

    private static String buildUserPrompt(String user, String userContext, List<Map<String, Object>> contextChunks) {
        String contextJson = toJson(contextChunks);
        return user + "\n\n" +
                (userContext != null && !userContext.isBlank()
                        ? ("User_context (private, for personalization; do not expose sensitive info):\n" + userContext + "\n\n")
                        : "") +
                "Retrieved_context_chunks (for grounding, cite by chunkId):\n" +
                contextJson;
    }

    private static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * Chat with strict JSON-object response format. Used by structured tasks (similar-listing
     * rationale, finetune evaluator).
     *
     * @param modelOverride when non-blank, replaces {@link #chatModel} — e.g. an OpenAI {@code ft:...}
     *                      fine-tuned model id. Pass {@code null} to keep using the default.
     */
    public String chatStructuredJson(String system, String userContent, String modelOverride) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("OPENAI_API_KEY is not configured");
        }
        String m = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : chatModel;

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", m);
        payload.put("temperature", 0.15);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", userContent)
        ));

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
