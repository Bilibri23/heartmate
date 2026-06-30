package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.AiChatRequest;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Native (in-Spring) agentic action router. Detects the three tenant write intents
 * (save / apply / request-visit) directly from the chat message and runs them through
 * {@link AiAgentToolService}, which re-enforces role and domain rules. This replaces the
 * external Python LangGraph sidecar for action execution, so the assistant can take actions
 * with {@code roombay.ai.orchestrator.enabled=false} and no extra service to deploy.
 *
 * <p>Intent regexes mirror the sidecar's {@code classify_intent} rules
 * (ai-orchestrator/app/nodes.py). Returns {@link Optional#empty()} for any non-action message
 * so the caller falls through to the read-only RAG/copilot pipeline unchanged.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAgentActionRouter {

    private static final Pattern UUID_RE = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static final Pattern REQUEST_VISIT = Pattern.compile(
            "(?i)\\b(request|schedule|book|arrange|set\\s*up)\\b.*\\b(visit|viewing|tour|inspection)\\b");
    private static final Pattern APPLY = Pattern.compile(
            "(?i)\\b(apply|submit\\s+(an?\\s+|my\\s+)?application)\\b.*"
                    + "\\b(this|that|listing|room|property|place|apartment|it)\\b");
    private static final Pattern SAVE = Pattern.compile(
            "(?i)\\b(save|favou?rite|bookmark|add\\s+to\\s+favou?rites?)\\b.*"
                    + "\\b(this|that|listing|room|property|place|apartment|it)\\b");

    /** Question lead-ins: keep how-to questions on the RAG path instead of treating them as actions. */
    private static final Pattern QUESTION_LEAD = Pattern.compile(
            "(?i)^\\s*(how|what|where|when|why|which|can|could|should|would|do|does|did|is|are|will|"
                    + "tell\\s+me|explain)\\b");

    private final AiAgentToolService aiAgentToolService;

    /**
     * @return a completed action response, or empty when the message is not a write action
     *         (caller continues with the normal pipeline).
     */
    public Optional<AiChatResponse> tryExecute(UUID userId, String role, AiChatRequest request, String threadId) {
        String message = request == null ? null : request.getMessage();
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        String tool = classifyTool(message);
        if (tool == null) {
            return Optional.empty();
        }

        UUID listingId = resolveListingId(message, request.getContextListingId());
        if (listingId == null) {
            // Imperative with no target → ask which listing. Question-shaped ("how do I apply?")
            // → fall through so RAG can explain.
            if (QUESTION_LEAD.matcher(message).find() || message.strip().endsWith("?")) {
                return Optional.empty();
            }
            return Optional.of(plainAnswer(threadId,
                    "Sure — which listing? Search for it here, paste its link, or open the listing and ask again."));
        }

        if (!"STUDENT".equalsIgnoreCase(role)) {
            return Optional.of(plainAnswer(threadId,
                    "Only tenant accounts can save listings, apply, or request visits from chat."));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("listingId", listingId.toString());
        if ("APPLY_TO_LISTING".equals(tool)) {
            params.put("message", message);
        }

        String requestId = UUID.randomUUID().toString();
        AiAgentToolService.ToolExecutionResult result =
                aiAgentToolService.execute(userId, role, tool, params, requestId);
        log.info("[AI][action] user={} tool={} listing={} success={}", userId, tool, listingId, result.success());

        AiChatResponse.ToolExecution exec = AiChatResponse.ToolExecution.builder()
                .tool(result.tool())
                .success(result.success())
                .message(result.message())
                .entityId(result.entityId())
                .build();

        return Optional.of(AiChatResponse.builder()
                .answer(result.message())
                .threadId(threadId)
                .citations(List.of())
                .suggestedActions(Boolean.TRUE.equals(result.success()) ? nextSteps(tool) : List.of())
                .listingResults(List.of())
                .ragGrounded(false)
                .toolExecutions(List.of(exec))
                .build());
    }

    private String classifyTool(String message) {
        if (REQUEST_VISIT.matcher(message).find()) {
            return "REQUEST_VISIT";
        }
        if (APPLY.matcher(message).find()) {
            return "APPLY_TO_LISTING";
        }
        if (SAVE.matcher(message).find()) {
            return "SAVE_LISTING_FAVORITE";
        }
        return null;
    }

    private UUID resolveListingId(String message, String contextListingId) {
        Matcher m = UUID_RE.matcher(message);
        if (m.find()) {
            UUID fromMessage = parseUuid(m.group());
            if (fromMessage != null) {
                return fromMessage;
            }
        }
        return parseUuid(contextListingId);
    }

    private List<AiChatResponse.SuggestedAction> nextSteps(String tool) {
        return switch (tool) {
            case "APPLY_TO_LISTING" -> List.of(navigate("view-applications", "View my applications", "/applications"));
            case "REQUEST_VISIT" -> List.of(navigate("view-visits", "View my visits", "/visits"));
            case "SAVE_LISTING_FAVORITE" -> List.of(navigate("view-favorites", "View saved listings", "/favorites"));
            default -> List.of();
        };
    }

    private AiChatResponse.SuggestedAction navigate(String id, String label, String url) {
        return AiChatResponse.SuggestedAction.builder()
                .id(id).label(label).type("NAVIGATE").actionUrl(url).build();
    }

    private AiChatResponse plainAnswer(String threadId, String text) {
        return AiChatResponse.builder()
                .answer(text)
                .threadId(threadId)
                .citations(List.of())
                .suggestedActions(List.of())
                .listingResults(List.of())
                .ragGrounded(false)
                .toolExecutions(List.of())
                .build();
    }

    private static UUID parseUuid(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
