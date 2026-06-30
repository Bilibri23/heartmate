package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.AiChatRequest;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private static final Pattern VERIFICATION_TYPE = Pattern.compile("(?i)\\b(identity|business|property)\\b");

    /** Ordered privileged-intent rules per role: regex (verb+noun) → tool + confirm-button label. */
    private record PrivilegedRule(Pattern pattern, String tool, String label) {}

    private static final List<PrivilegedRule> LANDLORD_RULES = List.of(
            new PrivilegedRule(Pattern.compile("(?i)\\b(reject|decline|deny|turn\\s*down)\\b.*\\bapplication"),
                    AiPrivilegedActionService.REJECT_APPLICATION, "Confirm: Reject application"),
            new PrivilegedRule(Pattern.compile("(?i)\\bshortlist\\b.*\\bapplication"),
                    AiPrivilegedActionService.SHORTLIST_APPLICATION, "Confirm: Shortlist application"),
            new PrivilegedRule(Pattern.compile("(?i)\\b(accept|approve)\\b.*\\bapplication"),
                    AiPrivilegedActionService.ACCEPT_APPLICATION, "Confirm: Accept application"),
            new PrivilegedRule(Pattern.compile("(?i)\\b(cancel|decline)\\b.*\\b(visit|viewing|tour)"),
                    AiPrivilegedActionService.CANCEL_VISIT, "Confirm: Cancel visit"),
            new PrivilegedRule(Pattern.compile("(?i)\\b(complete|completed|finish|done)\\b.*\\b(visit|viewing|tour)"
                    + "|\\bmark\\b.*\\b(visit|viewing).*\\b(complete|done)"),
                    AiPrivilegedActionService.COMPLETE_VISIT, "Confirm: Mark visit completed"),
            new PrivilegedRule(Pattern.compile("(?i)\\b(accept|approve|confirm)\\b.*\\b(visit|viewing|tour)"),
                    AiPrivilegedActionService.ACCEPT_VISIT, "Confirm: Accept visit"));

    private static final List<PrivilegedRule> ADMIN_RULES = List.of(
            new PrivilegedRule(Pattern.compile("(?i)\\b(reject|remove|take\\s*down|decline)\\b.*\\blisting"),
                    AiPrivilegedActionService.REJECT_LISTING, "Confirm: Reject listing"),
            new PrivilegedRule(Pattern.compile("(?i)\\b(approve|publish|activate)\\b.*\\blisting"),
                    AiPrivilegedActionService.APPROVE_LISTING, "Confirm: Approve listing"),
            new PrivilegedRule(Pattern.compile("(?i)\\b(reject|decline)\\b.*\\bverification"),
                    AiPrivilegedActionService.REJECT_VERIFICATION, "Confirm: Reject verification"),
            new PrivilegedRule(Pattern.compile("(?i)\\b(approve|verify)\\b.*\\bverification"),
                    AiPrivilegedActionService.APPROVE_VERIFICATION, "Confirm: Approve verification"),
            new PrivilegedRule(Pattern.compile("(?i)\\b(verify|confirm|approve)\\b.*\\bpayment"),
                    AiPrivilegedActionService.VERIFY_PAYMENT, "Confirm: Verify payment"),
            new PrivilegedRule(Pattern.compile("(?i)\\b(resolve|dismiss|close)\\b.*\\breport"),
                    AiPrivilegedActionService.RESOLVE_REPORT, "Confirm: Resolve report"));

    private final AiAgentToolService aiAgentToolService;
    private final AiPrivilegedActionService aiPrivilegedActionService;

    /**
     * @return a completed action response, or empty when the message is not a write action
     *         (caller continues with the normal pipeline).
     */
    public Optional<AiChatResponse> tryExecute(UUID userId, String role, AiChatRequest request, String threadId) {
        String message = request == null ? null : request.getMessage();
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        String normalizedRole = role == null ? "" : role.trim().toUpperCase(java.util.Locale.ROOT);
        if ("LANDLORD".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            return proposePrivilegedAction(normalizedRole, request, threadId);
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

    /**
     * Landlord/admin actions are NOT executed here — they are proposed with a CONFIRM_ACTION button.
     * The user must click confirm, which calls the execute endpoint ({@link AiPrivilegedActionService}).
     */
    private Optional<AiChatResponse> proposePrivilegedAction(String role, AiChatRequest request, String threadId) {
        String message = request.getMessage();
        List<PrivilegedRule> rules = "ADMIN".equals(role) ? ADMIN_RULES : LANDLORD_RULES;
        PrivilegedRule matched = null;
        for (PrivilegedRule rule : rules) {
            if (rule.pattern().matcher(message).find()) {
                matched = rule;
                break;
            }
        }
        if (matched == null) {
            return Optional.empty();
        }
        // Defensive: only propose tools the executor recognises for this role.
        boolean valid = "ADMIN".equals(role)
                ? aiPrivilegedActionService.isAdminTool(matched.tool())
                : aiPrivilegedActionService.isLandlordTool(matched.tool());
        if (!valid) {
            return Optional.empty();
        }

        boolean isListingTool = matched.tool().contains("LISTING");
        UUID targetId = resolvePrivilegedTarget(message, request.getContextListingId(), isListingTool);
        if (targetId == null) {
            if (QUESTION_LEAD.matcher(message).find() || message.strip().endsWith("?")) {
                return Optional.empty();
            }
            return Optional.of(plainAnswer(threadId,
                    "Which one? Paste the " + targetNoun(matched.tool())
                            + " id (it's shown on the item in your dashboard) and I'll set it up to confirm."));
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("targetId", targetId.toString());
        params.put("reason", message);
        if (matched.tool().contains("VERIFICATION")) {
            Matcher vt = VERIFICATION_TYPE.matcher(message);
            params.put("verificationType", vt.find() ? vt.group(1).toUpperCase(java.util.Locale.ROOT) : "IDENTITY");
        }

        AiChatResponse.SuggestedAction confirm = AiChatResponse.SuggestedAction.builder()
                .id("confirm-" + matched.tool().toLowerCase(java.util.Locale.ROOT))
                .label(matched.label())
                .type("CONFIRM_ACTION")
                .tool(matched.tool())
                .actionParams(params)
                .build();

        String plain = matched.label().replace("Confirm: ", "");
        String answer = plain + " for record " + shortId(targetId)
                + "? This acts on the live record. Click confirm to proceed, or ignore to cancel.";

        return Optional.of(AiChatResponse.builder()
                .answer(answer)
                .threadId(threadId)
                .citations(List.of())
                .suggestedActions(List.of(confirm))
                .listingResults(List.of())
                .ragGrounded(false)
                .toolExecutions(List.of())
                .build());
    }

    private UUID resolvePrivilegedTarget(String message, String contextListingId, boolean isListingTool) {
        Matcher m = UUID_RE.matcher(message);
        if (m.find()) {
            UUID fromMessage = parseUuid(m.group());
            if (fromMessage != null) {
                return fromMessage;
            }
        }
        return isListingTool ? parseUuid(contextListingId) : null;
    }

    private static String targetNoun(String tool) {
        if (tool.contains("APPLICATION")) return "application";
        if (tool.contains("VISIT")) return "visit";
        if (tool.contains("LISTING")) return "listing";
        if (tool.contains("VERIFICATION")) return "verification";
        if (tool.contains("PAYMENT")) return "payment";
        if (tool.contains("REPORT")) return "report";
        return "record";
    }

    private static String shortId(UUID id) {
        String s = id.toString();
        return s.substring(0, 8) + "…";
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
