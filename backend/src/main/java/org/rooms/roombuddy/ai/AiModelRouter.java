package org.rooms.roombuddy.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Routes AI calls to OpenAI (if configured) or Ollama (free local dev).
 */
@Component
@RequiredArgsConstructor
public class AiModelRouter {

    private final OpenAiClient openAiClient;
    private final OllamaClient ollamaClient;

    @Value("${OPENAI_API_KEY:}")
    private String openAiKey;

    @Value("${AI_PROVIDER:auto}")
    private String provider; // auto | openai | ollama

    public List<Double> embed(String input) {
        if (useOpenAi()) return openAiClient.embed(input);
        return ollamaClient.embed(input);
    }

    public String chat(String system, String user, List<Map<String, Object>> contextChunks, String userContext) {
        if (useOpenAi()) return openAiClient.chat(system, user, contextChunks, userContext);
        return ollamaClient.chat(system, user, userContext, contextChunks);
    }

    private boolean useOpenAi() {
        if ("openai".equalsIgnoreCase(provider)) return true;
        if ("ollama".equalsIgnoreCase(provider)) return false;
        return openAiKey != null && !openAiKey.isBlank();
    }
}

