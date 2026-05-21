package org.rooms.roombay.ai;

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

    /** Fine-tuned head for structured-JSON tasks only. Empty = reuse {@code OPENAI_MODEL}. */
    @Value("${OPENAI_STRUCTURED_MODEL:}")
    private String openAiStructuredModel;

    @Value("${OLLAMA_STRUCTURED_MODEL:}")
    private String ollamaStructuredModel;

    /** When set, RAG chat uses this model (e.g. Ollama fine-tuned tag) instead of the base chat model. */
    @Value("${OPENAI_FINETUNE_CHAT_MODEL:}")
    private String openAiFinetuneChatModel;

    @Value("${OLLAMA_FINETUNE_CHAT_MODEL:}")
    private String ollamaFinetuneChatModel;

    public List<Double> embed(String input) {
        if (useOpenAi()) return openAiClient.embed(input);
        return ollamaClient.embed(input);
    }

    public String chat(String system, String user, List<Map<String, Object>> contextChunks, String userContext) {
        if (useOpenAi()) {
            String override = (openAiFinetuneChatModel != null && !openAiFinetuneChatModel.isBlank())
                    ? openAiFinetuneChatModel : null;
            return openAiClient.chat(system, user, contextChunks, userContext, override);
        }
        String override = (ollamaFinetuneChatModel != null && !ollamaFinetuneChatModel.isBlank())
                ? ollamaFinetuneChatModel : null;
        return ollamaClient.chat(system, user, userContext, contextChunks, override);
    }

    /**
     * Structured JSON generation (no RAG). Routes to {@link #openAiStructuredModel} or
     * {@link #ollamaStructuredModel} when set, so a fine-tuned head can serve structured tasks
     * while the main assistant keeps the base chat model.
     */
    public String chatStructuredJson(String system, String userJson) {
        if (useOpenAi()) {
            String override = openAiStructuredModel != null && !openAiStructuredModel.isBlank()
                    ? openAiStructuredModel : null;
            return openAiClient.chatStructuredJson(system, userJson, override);
        }
        String override = ollamaStructuredModel != null && !ollamaStructuredModel.isBlank()
                ? ollamaStructuredModel : null;
        return ollamaClient.chatStructured(system, userJson, override);
    }

    private boolean useOpenAi() {
        if ("openai".equalsIgnoreCase(provider)) return true;
        if ("ollama".equalsIgnoreCase(provider)) return false;
        return openAiKey != null && !openAiKey.isBlank();
    }
}

