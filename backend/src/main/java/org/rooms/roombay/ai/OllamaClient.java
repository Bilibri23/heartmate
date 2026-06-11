package org.rooms.roombay.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Minimal Ollama HTTP client for local, free dev.
 * Requires: ollama running (default http://localhost:11434).
 *
 * Env vars:
 * - OLLAMA_BASE_URL (default http://localhost:11434)
 * - OLLAMA_CHAT_MODEL (default llama3.2)
 * - OLLAMA_FINETUNE_CHAT_MODEL (optional; overrides chat model for RAG assistant)
 * - OLLAMA_EMBEDDING_MODEL (default nomic-embed-text)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaClient {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient.Builder restClientBuilder;

    @Value("${OLLAMA_BASE_URL:http://localhost:11434}")
    private String baseUrl;

    @Value("${OLLAMA_CHAT_MODEL:llama3.2}")
    private String chatModel;

    @Value("${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}")
    private String embeddingModel;

    @Value("${roombay.ai.debug-synthesis-logging:false}")
    private boolean debugSynthesisLogging;

    public List<Double> embed(String input) {
        Map<String, Object> payload = Map.of("model", embeddingModel, "input", input);

        var client = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<?, ?> res;
        try {
            res = client.post()
                    .uri("/api/embed")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            String fallbackModel = chooseFallbackEmbeddingModel(client, embeddingModel);
            if (fallbackModel == null) {
                throw new BadRequestException("Ollama embedding model not found: '" + embeddingModel + "'. Pull one (e.g. `ollama pull nomic-embed-text`) or set OLLAMA_EMBEDDING_MODEL.");
            }
            log.warn("[AI] Ollama embedding model '{}' missing. Falling back to '{}'.", embeddingModel, fallbackModel);
            res = client.post()
                    .uri("/api/embed")
                    .body(Map.of("model", fallbackModel, "input", input))
                    .retrieve()
                    .body(Map.class);
        }

        if (res == null) throw new BadRequestException("Ollama embedding request failed");
        List<?> embeddings = (List<?>) res.get("embeddings");
        if (embeddings == null || embeddings.isEmpty()) throw new BadRequestException("Ollama embedding response was empty");
        List<?> vec0 = (List<?>) embeddings.get(0);
        if (vec0 == null || vec0.isEmpty()) throw new BadRequestException("Ollama embedding missing in response");
        return vec0.stream().map(v -> ((Number) v).doubleValue()).toList();
    }

    public String chat(String system, String user, String userContext, List<Map<String, Object>> contextChunks) {
        return chat(system, user, userContext, contextChunks, null);
    }

    /**
     * @param modelOverride when non-blank, used instead of {@link #chatModel} (e.g. fine-tuned Ollama tag).
     */
    public String chat(String system, String user, String userContext, List<Map<String, Object>> contextChunks,
                       String modelOverride) {
        String m = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : chatModel;
        String prompt = buildPrompt(system, user, userContext, contextChunks);

        Map<String, Object> payload = Map.of("model", m, "prompt", prompt, "stream", false);
        if (debugSynthesisLogging) {
            log.info("[AI] Ollama final prompt model={} prompt=\n{}", m, prompt);
        }

        var client = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<?, ?> res;
        try {
            res = client.post()
                    .uri("/api/generate")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            String fallbackModel = chooseFallbackChatModel(client, m);
            if (fallbackModel == null) {
                throw new BadRequestException("Ollama chat model not found: '" + m + "'. Pull a model first (e.g. `ollama pull llama3.2`) or set OLLAMA_CHAT_MODEL / OLLAMA_FINETUNE_CHAT_MODEL.");
            }
            log.warn("[AI] Ollama model '{}' missing. Falling back to '{}'.", m, fallbackModel);
            res = client.post()
                    .uri("/api/generate")
                    .body(Map.of("model", fallbackModel, "prompt", prompt, "stream", false))
                    .retrieve()
                    .body(Map.class);
        }

        if (res == null) throw new BadRequestException("Ollama chat request failed");
        String response = (String) res.get("response");
        if (response == null) throw new BadRequestException("Ollama chat response missing 'response'");
        return response.trim();
    }

    public String chatStream(String system, String user, String userContext, List<Map<String, Object>> contextChunks,
                             String modelOverride, Consumer<String> onToken) {
        String m = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : chatModel;
        String prompt = buildPrompt(system, user, userContext, contextChunks);
        try {
            return streamGenerate(m, prompt, onToken);
        } catch (BadRequestException ex) {
            if (ex.getMessage() == null || !ex.getMessage().contains("not found")) {
                throw ex;
            }
            var client = restClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            String fallbackModel = chooseFallbackChatModel(client, m);
            if (fallbackModel == null || fallbackModel.equals(m)) {
                throw ex;
            }
            log.warn("[AI] Ollama model '{}' missing for stream. Falling back to '{}'.", m, fallbackModel);
            return streamGenerate(fallbackModel, prompt, onToken);
        }
    }

    private String streamGenerate(String model, String prompt, Consumer<String> onToken) {
        try {
            String body = JSON.writeValueAsString(Map.of("model", model, "prompt", prompt, "stream", true));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<java.io.InputStream> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 404) {
                throw new BadRequestException("Ollama chat model not found: '" + model + "'.");
            }
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Ollama streaming request failed with status " + response.statusCode());
            }

            StringBuilder full = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    var node = JSON.readTree(line);
                    String token = node.path("response").asText("");
                    if (!token.isEmpty()) {
                        full.append(token);
                        if (onToken != null) {
                            onToken.accept(token);
                        }
                    }
                    if (node.path("done").asBoolean(false)) {
                        break;
                    }
                }
            }
            if (full.isEmpty()) {
                throw new BadRequestException("Ollama streaming response was empty");
            }
            return full.toString().trim();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Ollama streaming failed: " + ex.getMessage());
        }
    }

    private static String buildPrompt(String system, String user, String userContext, List<Map<String, Object>> contextChunks) {
        String contextJson = toJson(contextChunks);
        return system + "\n\n" +
                "User_question:\n" + user + "\n\n" +
                (userContext != null && !userContext.isBlank()
                        ? ("User_context (private):\n" + userContext + "\n\n")
                        : "") +
                "Retrieved_context_chunks:\n" + contextJson + "\n\n" +
                "Answer:";
    }

    private static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * Structured JSON task without RAG. Honours a per-call model override so a local
     * fine-tuned tag (e.g. {@code roombay-rationale:latest}) can be used for structured tasks
     * while the main assistant keeps using {@link #chatModel}.
     */
    public String chatStructured(String system, String userJson, String modelOverride) {
        String m = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : chatModel;
        String prompt = system + "\n\nInput_JSON:\n" + userJson + "\n\nReply with a single JSON object only, no markdown.";

        Map<String, Object> payload = Map.of("model", m, "prompt", prompt, "stream", false);

        var client = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<?, ?> res;
        try {
            res = client.post()
                    .uri("/api/generate")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            String fallbackModel = chooseFallbackChatModel(client, m);
            if (fallbackModel == null) {
                throw new BadRequestException("Ollama model not found: '" + m + "'. Pull a model or set OLLAMA_STRUCTURED_MODEL / OLLAMA_CHAT_MODEL.");
            }
            log.warn("[AI] Ollama structured model '{}' missing. Falling back to '{}'.", m, fallbackModel);
            res = client.post()
                    .uri("/api/generate")
                    .body(Map.of("model", fallbackModel, "prompt", prompt, "stream", false))
                    .retrieve()
                    .body(Map.class);
        }

        if (res == null) throw new BadRequestException("Ollama chat request failed");
        String response = (String) res.get("response");
        if (response == null) throw new BadRequestException("Ollama chat response missing 'response'");
        return response.trim();
    }

    private String chooseFallbackChatModel(RestClient client, String preferredModel) {
        try {
            Map<?, ?> res = client.get().uri("/api/tags").retrieve().body(Map.class);
            if (res == null) return null;
            List<?> models = (List<?>) res.get("models");
            if (models == null || models.isEmpty()) return null;

            List<String> names = new ArrayList<>();
            for (Object m : models) {
                if (m instanceof Map<?, ?> mm) {
                    Object n = mm.get("name");
                    if (n instanceof String s && !s.isBlank()) {
                        names.add(s);
                    }
                }
            }
            if (names.isEmpty()) return null;
            if (names.contains(preferredModel)) return preferredModel;
            for (String name : names) {
                if (name.startsWith("llama")) return name;
            }
            return names.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private String chooseFallbackEmbeddingModel(RestClient client, String preferredModel) {
        try {
            Map<?, ?> res = client.get().uri("/api/tags").retrieve().body(Map.class);
            if (res == null) return null;
            List<?> models = (List<?>) res.get("models");
            if (models == null || models.isEmpty()) return null;

            List<String> names = new ArrayList<>();
            for (Object m : models) {
                if (m instanceof Map<?, ?> mm) {
                    Object n = mm.get("name");
                    if (n instanceof String s && !s.isBlank()) {
                        names.add(s);
                    }
                }
            }
            if (names.isEmpty()) return null;
            if (names.contains(preferredModel)) return preferredModel;
            for (String name : names) {
                if (name.contains("embed")) return name;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
