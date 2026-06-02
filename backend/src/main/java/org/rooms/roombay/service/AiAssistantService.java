package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.ai.AiModelRouter;
import org.rooms.roombay.ai.rag.AiGraphRagService;
import org.rooms.roombay.ai.rag.AiRagRepository;
import org.rooms.roombay.ai.safety.AiContextSanitizer;
import org.rooms.roombay.ai.safety.AiOutputGuard;
import org.rooms.roombay.ai.safety.AiRetrievalPolicy;
import org.rooms.roombay.ai.safety.AiSafetyClassifier;
import org.rooms.roombay.config.AiChatRateLimiter;
import org.rooms.roombay.dto.request.AiChatRequest;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.repository.AiChatLogRepository;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.LeaseRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.rooms.roombay.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_VISIBLE_ANSWER_CHARS = 520;
    private static final Pattern NEXT_STEP_PATTERN = Pattern.compile("(?i)\\bnext\\s*step\\b");
    static final String NO_DOC_FALLBACK_ANSWER =
            "I do not have documentation that answers this question. Please open the relevant RoomBay screen or contact support.";

    private final AiModelRouter modelRouter;
    private final AiGraphRagService graphRagService;
    private final StudentVerificationRepository studentVerificationRepository;
    private final RoomApplicationRepository roomApplicationRepository;
    private final LeaseRepository leaseRepository;
    private final PropertyListingRepository propertyListingRepository;
    private final LandlordVerificationRepository landlordVerificationRepository;
    private final AiChatLogRepository chatLogRepository;
    private final AiChatRateLimiter aiChatRateLimiter;
    private final AiMemoryService aiMemoryService;
    private final AiContextSanitizer contextSanitizer;
    private final AiSafetyClassifier safetyClassifier;
    private final AiOutputGuard outputGuard;
    private final AiRetrievalPolicy retrievalPolicy;
    private final AnalyticsEventService analyticsEventService;
    private final AppErrorLogService appErrorLogService;

    @Value("${roombay.ai.memory.max-turns:6}")
    private int memoryMaxTurns;

    @Value("${roombay.ai.debug-synthesis-logging:false}")
    private boolean debugSynthesisLogging;

    public AiChatResponse chat(AiChatRequest request) {
        long started = System.currentTimeMillis();
        UUID userId = SecurityUtils.getCurrentUserId(); // ensure authenticated
        aiChatRateLimiter.checkAllowed(userId);
        String threadId = resolveThreadId(request.getThreadId());
        AiChatRequest.Persona persona = resolveEffectivePersona(request);
        analyticsEventService.emit("ai_question_asked", userId, SecurityUtils.getCurrentUserRole(), null,
                Map.of("persona", persona.name()));

        // Layer 4 (input side): deterministic refusal short-circuit. Model is also finetuned
        // to reproduce the same refusal copy, so the user-visible behaviour is consistent.
        AiSafetyClassifier.Decision safety = safetyClassifier.classify(request.getMessage());
        if (safety != AiSafetyClassifier.Decision.ALLOW) {
            String refusalText = safetyClassifier.refusalText(safety);
            log.info("[AI] safety short-circuit user={} decision={}", userId, safety);
            chatLogRepository.insert(userId, persona.name(),
                    request.getMessage(), refusalText, List.of());
            aiMemoryService.appendTurn(userId, threadId, request.getMessage(), refusalText);
            analyticsEventService.emit("ai_no_answer", userId, SecurityUtils.getCurrentUserRole(), null,
                    Map.of("reason", "safety_refusal", "persona", persona.name(), "question", safeAnalyticsQuestion(request.getMessage())));
            return AiChatResponse.builder()
                    .answer(refusalText)
                    .threadId(threadId)
                    .citations(List.of())
                    .suggestedActions(List.of())
                    .ragGrounded(false)
                    .build();
        }

        // Persona is derived from authenticated role to prevent client spoofing.
        List<Double> qEmb = modelRouter.embed(request.getMessage());
        List<AiRagRepository.ChunkRow> chunks;
        try {
            chunks = graphRagService.retrieve(qEmb, 8);
        } catch (Exception e) {
            // Keep assistant available even when vector index/schema has drift.
            log.warn("[AI] RAG retrieval failed, continuing without context: {}", e.getMessage());
            chunks = List.of();
        }

        // Layer 1: drop chunks the caller's role isn't authorized to see.
        chunks = retrievalPolicy.filter(chunks, SecurityUtils.getCurrentUserRole());
        logRetrievedChunks(userId, chunks);
        boolean ragGrounded = !chunks.isEmpty();
        if (!ragGrounded) {
            log.info("[AI] chat user={} ragChunks=0 grounded=false graphRag={} ms={}",
                    userId, graphRagService.isGraphRagEnabled(), System.currentTimeMillis() - started);
            chatLogRepository.insert(userId, persona.name(), request.getMessage(), NO_DOC_FALLBACK_ANSWER, List.of());
            aiMemoryService.appendTurn(userId, threadId, request.getMessage(), NO_DOC_FALLBACK_ANSWER);
            analyticsEventService.emit("ai_no_answer", userId, SecurityUtils.getCurrentUserRole(), null,
                    Map.of("reason", "no_docs", "persona", persona.name(), "question", safeAnalyticsQuestion(request.getMessage())));
            return AiChatResponse.builder()
                    .answer(NO_DOC_FALLBACK_ANSWER)
                    .threadId(threadId)
                    .citations(List.of())
                    .suggestedActions(List.of())
                    .ragGrounded(false)
                    .build();
        }

        // Layer 2: sanitize chunk text + user_context before they reach the model.
        List<Map<String, Object>> contextChunks = chunks.stream().map(c -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("chunkId", c.getId().toString());
            m.put("source", c.getSource());
            m.put("title", c.getTitle());
            m.put("text", contextSanitizer.sanitize(c.getChunkText()));
            return m;
        }).toList();

        String system = buildSystemPrompt(persona, ragGrounded);
        String userContext = contextSanitizer.sanitize(buildUserContext(userId, persona));
        List<AiMemoryService.MemoryTurn> memoryTurns = aiMemoryService.loadRecentTurns(userId, threadId, memoryMaxTurns);
        String memoryContext = contextSanitizer.sanitize(buildMemoryContext(memoryTurns));
        String fullUserContext = memoryContext.isBlank() ? userContext : userContext + "\n\n" + memoryContext;
        logPromptAssembly(userId, system, request.getMessage(), contextChunks, fullUserContext);
        ParsedAssistant parsed;
        try {
            String raw = modelRouter.chat(system, request.getMessage(), contextChunks, fullUserContext);
            logRawModelResponse(userId, raw);
            parsed = parseAssistantOutput(raw);
            logParsedResponse(userId, parsed);
        } catch (RuntimeException ex) {
            appErrorLogService.log("ERROR", "AI", "AI chat synthesis failed: " + ex.getMessage(), "/api/ai/chat", ex);
            throw ex;
        }

        List<AiChatResponse.Citation> citations = chunks.stream().map(c -> AiChatResponse.Citation.builder()
                .chunkId(c.getId().toString())
                .source(c.getSource())
                .title(c.getTitle())
                .build()
        ).toList();

        // Layer 3: post-generation guard. Replaces leaked PII or unsupported internal claims
        // with the canonical RoomBay fallback before the answer ever reaches the user.
        String styledAnswer = enforceExpectedChatStyle(
                ensureNonEmptyAnswer(parsed.answer, ragGrounded),
                persona,
                ragGrounded,
                request.getMessage()
        );
        AiOutputGuard.GuardResult guard = outputGuard.guard(styledAnswer);
        if (guard.redacted()) {
            log.warn("[AI] output guard redacted answer reason={}", guard.reason());
        }
        String safeAnswer = guard.safeText();

        AiChatResponse response = AiChatResponse.builder()
                .answer(safeAnswer)
                .threadId(threadId)
                .citations(citations)
                .suggestedActions(guard.redacted() ? List.of() : parsed.actions)
                .ragGrounded(ragGrounded)
                .build();

        log.info("[AI] chat user={} ragChunks={} grounded={} graphRag={} ms={}",
                userId, chunks.size(), ragGrounded, graphRagService.isGraphRagEnabled(), System.currentTimeMillis() - started);

        chatLogRepository.insert(
                userId,
                persona.name(),
                request.getMessage(),
                response.getAnswer(),
                citations.stream().map(AiChatResponse.Citation::getChunkId).toList()
        );
        aiMemoryService.appendTurn(userId, threadId, request.getMessage(), response.getAnswer());
        analyticsEventService.emit(
                NO_DOC_FALLBACK_ANSWER.equals(response.getAnswer()) ? "ai_no_answer" : "ai_answer_returned",
                userId,
                SecurityUtils.getCurrentUserRole(),
                null,
                Map.of("persona", persona.name(), "ragGrounded", ragGrounded, "question", safeAnalyticsQuestion(request.getMessage()))
        );

        return response;
    }

    private String buildSystemPrompt(AiChatRequest.Persona persona, boolean hasDocContext) {
        String personaLine;
        if (persona == AiChatRequest.Persona.ADMIN) {
            personaLine = String.join("\n",
                    "You are helping a RoomBay platform administrator.",
                    "Admin mode: ops-level, queue-aware, diagnostic, and runbook-oriented.",
                    "Admins may receive admin docs, internal runbooks, and security guidance, but never secrets, raw tokens, private documents, or unnecessary PII."
            );
        } else if (persona == AiChatRequest.Persona.LANDLORD) {
            personaLine = String.join("\n",
                    "You are helping a landlord user of RoomBay.",
                    "Landlord mode: operational, dashboard-focused, and practical for listings, applications, leases, verification, payments, and payouts.",
                    "Do not expose tenant verification document packets or internal admin notes."
            );
        } else {
            personaLine = String.join("\n",
                    "You are helping a tenant user of RoomBay.",
                    "Tenant mode: clear, safe, action-oriented, and focused on finding homes, applications, verification, leases, payments, roommates, and support.",
                    "Do not expose landlord private documents, internal admin notes, or privileged operational details."
            );
        }

        String noKb = "";
        if (!hasDocContext) {
            noKb = String.join("\n",
                    "",
                    "Critical: No documentation chunks were retrieved from the RoomBay knowledge base for this question.",
                    "- Use exactly this answer and do not add tips: " + NO_DOC_FALLBACK_ANSWER
            );
        }

        return String.join("\n",
                "You are RoomBay Assistant for the RoomBay platform.",
                personaLine,
                noKb,
                "",
                "Terminology (do not confuse these):",
                "- Listing verification / listing trust: whether a property listing or landlord has been reviewed or verified for the marketplace (badges on listing cards, landlord verification, admin review).",
                "- Tenant / student verification: the renter's identity verification for applying or safety — not the same as listing verification.",
                "- If the user asks how to know listings are verified, explain listing trust signals and landlord/listing verification — do NOT pivot to tenant ID upload unless they asked about tenant/student verification.",
                "",
                "Rules:",
                "- Only answer questions about RoomBay features, workflows, and policies.",
                "- Use retrieved context chunks and User_context lines to ground your answer; do not invent features, prices, policies, dashboards, or legal terms.",
                "- Match the user's authenticated role. Tenant answers use tenant/public docs; landlord answers use landlord/public docs; admin answers may use all docs.",
                "- If retrieved docs conflict with user context, say what is documented and avoid guessing.",
                "- Keep the visible answer short and practical (normally 2-5 sentences).",
                "- End with a clear 'Next step:' line whenever relevant.",
                "- Never claim that landlords view, receive, or verify tenant government ID or selfie uploads; tenant verification documents are reviewed by admin. Landlords see application and lease-related information as the app provides — not the tenant verification document packet.",
                "- For “what makes RoomBay unique” or differentiation questions, prioritize: (1) vision — home discovery as easy as booking a ride or shopping on Amazon; (2) feed/reels-style low-friction discovery (TikTok-like skim) and very fast first-pass decisions; (3) trust layers (admin-reviewed verification, leases) without misstating landlord access to tenant ID docs.",
                "- If the answer isn't in the context, say so briefly and point to a relevant in-app screen.",
                "- Keep answers concise and actionable.",
                "- Do not repeat internal labels like SUGGESTIONS_JSON or JSON ACTION in the user-facing answer.",
                "",
                "Output format (strict):",
                "1) Write only the human-readable answer in plain language (no markdown headings required).",
                "2) Immediately on the next line, exactly: SUGGESTED_ACTIONS_JSON: followed by a single JSON array (no other text after the array).",
                "Each action: { \"id\": \"string\", \"label\": \"string\", \"type\": \"NAVIGATE|COPY_TEXT\", \"actionUrl\": \"string?\", \"copyText\": \"string?\" }",
                "Example last line: SUGGESTED_ACTIONS_JSON: [{\"id\":\"search\",\"label\":\"Open search\",\"type\":\"NAVIGATE\",\"actionUrl\":\"/search\"}]"
        );
    }

    private String buildUserContext(UUID userId, AiChatRequest.Persona persona) {
        List<String> lines = new ArrayList<>();
        String role = SecurityUtils.getCurrentUserRole();
        lines.add("role=" + role);
        lines.add("persona=" + persona.name());

        if (persona == AiChatRequest.Persona.ADMIN) {
            lines.add("adminContext=true");
            lines.add("adminSafety=never expose private user data, verification docs, or internal-only notes in chat");
            return String.join("\n", lines);
        }

        if (persona == AiChatRequest.Persona.TENANT) {
            var v = studentVerificationRepository.findByUserId(userId);
            lines.add("tenantStudentVerificationStatus=" + v.map(ver -> ver.getStatus().name()).orElse("NONE"));
            lines.add("applicationsTotal=" + roomApplicationRepository.countByStudentId(userId));
            lines.add("applicationsPending=" + roomApplicationRepository.countByStudentIdAndStatus(userId, RoomApplication.Status.PENDING));
            lines.add("leasesTotal=" + leaseRepository.countByStudentId(userId));
        } else {
            lines.add("listingsTotal=" + propertyListingRepository.countByLandlordId(userId));
            lines.add("listingsMarkedVerifiedCount=" + propertyListingRepository.countByLandlordIdAndVerifiedTrue(userId));
            landlordVerificationRepository.findByUserId(userId).ifPresentOrElse(lv -> {
                lines.add("landlordVerificationLevel=" + lv.getVerificationLevel().name());
                lines.add("landlordIdentityStatus=" + lv.getIdentityStatus().name());
                lines.add("landlordBusinessStatus=" + lv.getBusinessStatus().name());
                lines.add("landlordPropertyStatus=" + lv.getPropertyStatus().name());
            }, () -> lines.add("landlordKyc=NONE"));
            lines.add("applicationsTotal=" + roomApplicationRepository.countByLandlordId(userId));
            lines.add("applicationsPending=" + roomApplicationRepository.countByLandlordIdAndStatus(userId, RoomApplication.Status.PENDING));
            lines.add("leasesTotal=" + leaseRepository.countByLandlordId(userId));
        }

        return String.join("\n", lines);
    }

    private String buildMemoryContext(List<AiMemoryService.MemoryTurn> turns) {
        if (turns == null || turns.isEmpty()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        lines.add("Conversation_memory_recent_turns:");
        int index = 1;
        for (AiMemoryService.MemoryTurn turn : turns) {
            lines.add(index + ". user: " + turn.userMessage());
            lines.add(index + ". assistant: " + turn.assistantAnswer());
            index++;
        }
        return String.join("\n", lines);
    }

    private String resolveThreadId(String requestThreadId) {
        if (requestThreadId == null || requestThreadId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestThreadId.trim();
    }

    private static AiChatRequest.Persona resolveEffectivePersona(AiChatRequest request) {
        String role = SecurityUtils.getCurrentUserRole();
        if ("ADMIN".equalsIgnoreCase(role)) {
            return AiChatRequest.Persona.ADMIN;
        }
        if ("LANDLORD".equalsIgnoreCase(role)) {
            return AiChatRequest.Persona.LANDLORD;
        }
        return AiChatRequest.Persona.TENANT;
    }

    private static String safeAnalyticsQuestion(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String safe = message
                .replaceAll("(?i)bearer\\s+[a-z0-9._\\-]+", "Bearer [redacted]")
                .replaceAll("(?i)(password|token|secret|authorization|cookie)\\s*[:=]\\s*\\S+", "$1=[redacted]")
                .replaceAll("[\\r\\n\\t]+", " ")
                .trim();
        return safe.length() <= 180 ? safe : safe.substring(0, 180);
    }

    private String ensureNonEmptyAnswer(String answer, boolean ragGrounded) {
        if (answer != null && !answer.isBlank()) {
            return answer.trim();
        }
        if (ragGrounded) {
            log.warn("[AI] Grounded retrieval produced blank parsed answer; using formatting fallback.");
            return "I found related RoomBay information, but I could not format a full answer this time. Please ask again in one sentence, and I will answer clearly with the same sources.";
        }
        return NO_DOC_FALLBACK_ANSWER;
    }

    @lombok.Value
    private static class ParsedAssistant {
        String answer;
        List<AiChatResponse.SuggestedAction> actions;
    }

    private ParsedAssistant parseAssistantOutput(String raw) {
        if (raw == null) return new ParsedAssistant("", List.of());
        String trimmed = raw.trim();
        ParsedAssistant jsonParsed = parseJsonAssistantOutput(trimmed);
        if (jsonParsed != null) {
            return jsonParsed;
        }
        String[] markers = { "SUGGESTED_ACTIONS_JSON:", "SUGGESTIONS_JSON:", "SUGGESTED_ACTIONS:" };
        int bestIdx = -1;
        String bestMarker = null;
        for (String m : markers) {
            int i = trimmed.lastIndexOf(m);
            if (i > bestIdx) {
                bestIdx = i;
                bestMarker = m;
            }
        }
        if (bestIdx < 0 || bestMarker == null) {
            return new ParsedAssistant(sanitizeAnswerText(trimmed), List.of());
        }
        String answerPart = sanitizeAnswerText(trimmed.substring(0, bestIdx));
        String json = trimmed.substring(bestIdx + bestMarker.length()).trim();
        int lb = json.indexOf('[');
        int rb = json.lastIndexOf(']');
        if (lb >= 0 && rb > lb) {
            json = json.substring(lb, rb + 1);
        }
        try {
            var listType = JSON.getTypeFactory().constructCollectionType(List.class, AiChatResponse.SuggestedAction.class);
            List<AiChatResponse.SuggestedAction> actions = JSON.readValue(json, listType);
            return new ParsedAssistant(answerPart, actions != null ? actions : List.of());
        } catch (Exception e) {
            log.debug("[AI] Could not parse suggested actions JSON: {}", e.getMessage());
            return new ParsedAssistant(answerPart.isBlank() ? sanitizeAnswerText(trimmed) : answerPart, List.of());
        }
    }

    private ParsedAssistant parseJsonAssistantOutput(String text) {
        String candidate = unwrapJsonCandidate(text);
        if (candidate == null || !candidate.startsWith("{")) {
            return null;
        }
        try {
            JsonNode root = JSON.readTree(candidate);
            String answer = firstText(root, "answer", "content", "message", "response", "text");
            List<AiChatResponse.SuggestedAction> actions = List.of();
            JsonNode actionNode = firstNode(root, "suggestedActions", "suggested_actions", "actions");
            if (actionNode != null && actionNode.isArray()) {
                var listType = JSON.getTypeFactory().constructCollectionType(List.class, AiChatResponse.SuggestedAction.class);
                actions = JSON.convertValue(actionNode, listType);
            }
            if (answer != null || !actions.isEmpty()) {
                return new ParsedAssistant(sanitizeAnswerText(answer == null ? "" : answer), actions);
            }
        } catch (Exception e) {
            log.debug("[AI] Raw model response was not parseable assistant JSON: {}", e.getMessage());
        }
        return null;
    }

    private static String unwrapJsonCandidate(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("(?is)^```(?:json)?\\s*", "");
            int fence = t.lastIndexOf("```");
            if (fence >= 0) {
                t = t.substring(0, fence);
            }
            t = t.trim();
        }
        int first = t.indexOf('{');
        int last = t.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return t.substring(first, last + 1).trim();
        }
        return t;
    }

    private static String firstText(JsonNode root, String... names) {
        JsonNode node = firstNode(root, names);
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private static JsonNode firstNode(JsonNode root, String... names) {
        if (root == null) return null;
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && !node.isNull()) return node;
        }
        return null;
    }

    /** Remove model junk lines and internal chunk references from the visible answer. */
    private static String sanitizeAnswerText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] lines = text.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) {
                sb.append("\n");
                continue;
            }
            String upper = t.toUpperCase();
            if (upper.startsWith("SUGGESTIONS:") && upper.contains("JSON")) continue;
            if (t.contains("[SUGGESTIONS_JSON]")) continue;
            if (t.contains("(JSON ACTION)")) continue;
            if (upper.contains("SUGGESTED_ACTIONS_JSON")) continue;
            sb.append(line).append("\n");
        }
        String s = sb.toString().trim();
        s = s.replaceAll("(?i)\\(chunkId:\\s*[a-f0-9\\-]{36}\\)\\s*", "");
        return s.trim();
    }

    private void logRetrievedChunks(UUID userId, List<AiRagRepository.ChunkRow> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            log.info("[AI] retrieved chunks user={} count=0", userId);
            return;
        }
        log.info("[AI] retrieved chunks user={} count={} sources={}", userId, chunks.size(),
                chunks.stream().map(c -> c.getSource() + "#" + c.getChunkIndex()).toList());
        if (debugSynthesisLogging) {
            chunks.forEach(c -> log.info("[AI] retrieved chunk detail user={} chunkId={} source={} title={} text={}",
                    userId, c.getId(), c.getSource(), c.getTitle(), c.getChunkText()));
        }
    }

    private void logPromptAssembly(UUID userId, String system, String userMessage, List<Map<String, Object>> contextChunks, String userContext) {
        int contextTextChars = contextChunks == null ? 0 : contextChunks.stream()
                .mapToInt(c -> String.valueOf(c.getOrDefault("text", "")).length())
                .sum();
        log.info("[AI] prompt assembly user={} contextChunks={} contextTextChars={} userContextChars={} systemChars={} questionChars={}",
                userId,
                contextChunks == null ? 0 : contextChunks.size(),
                contextTextChars,
                userContext == null ? 0 : userContext.length(),
                system == null ? 0 : system.length(),
                userMessage == null ? 0 : userMessage.length());
        if (debugSynthesisLogging) {
            log.info("[AI] final prompt parts user={} system=\n{}\nUser_question:\n{}\nUser_context:\n{}\nRetrieved_context_chunks:\n{}",
                    userId, system, userMessage, userContext, contextChunks);
        }
    }

    private void logRawModelResponse(UUID userId, String raw) {
        log.info("[AI] raw model response user={} chars={} blank={}", userId, raw == null ? 0 : raw.length(), raw == null || raw.isBlank());
        if (debugSynthesisLogging) {
            log.info("[AI] raw model response detail user={} raw=\n{}", userId, raw);
        }
    }

    private void logParsedResponse(UUID userId, ParsedAssistant parsed) {
        log.info("[AI] parsed model response user={} answerChars={} actions={} blankAnswer={}",
                userId,
                parsed == null || parsed.answer == null ? 0 : parsed.answer.length(),
                parsed == null || parsed.actions == null ? 0 : parsed.actions.size(),
                parsed == null || parsed.answer == null || parsed.answer.isBlank());
        if (debugSynthesisLogging && parsed != null) {
            log.info("[AI] parsed model response detail user={} answer=\n{}\nactions={}", userId, parsed.answer, parsed.actions);
        }
    }

    /**
     * Last-mile response shaping so live chat output matches product expectations:
     * short, practical, and with a clear next action.
     */
    private String enforceExpectedChatStyle(
            String answer,
            AiChatRequest.Persona persona,
            boolean ragGrounded,
            String userMessage) {
        if (answer == null || answer.isBlank()) {
            return answer;
        }
        if (!ragGrounded) {
            return NO_DOC_FALLBACK_ANSWER;
        }
        String out = answer.trim();
        out = out.replaceAll("\\n{3,}", "\n\n");

        if (out.length() > MAX_VISIBLE_ANSWER_CHARS) {
            out = trimToSentenceBoundary(out, MAX_VISIBLE_ANSWER_CHARS);
        }

        if (!NEXT_STEP_PATTERN.matcher(out).find()) {
            out = out + "\n\nNext step: " + suggestNextStep(persona, ragGrounded, userMessage);
        }
        return out.trim();
    }

    private static String trimToSentenceBoundary(String text, int maxChars) {
        if (text.length() <= maxChars) return text;
        int cut = text.lastIndexOf('.', maxChars);
        if (cut < maxChars / 2) {
            cut = text.lastIndexOf('\n', maxChars);
        }
        if (cut < maxChars / 2) {
            cut = maxChars;
        }
        return text.substring(0, cut).trim();
    }

    private static String suggestNextStep(AiChatRequest.Persona persona, boolean ragGrounded, String userMessage) {
        String msg = userMessage == null ? "" : userMessage.toLowerCase();
        if (msg.contains("trust") || msg.contains("scam") || msg.contains("safe")) {
            return "use in-app messaging, request a viewing time, and verify the listing badge before any payment.";
        }
        if (msg.contains("pay") || msg.contains("payment") || msg.contains("rent")) {
            return "complete payment inside RoomBay so your transaction stays protected.";
        }
        if (!ragGrounded) {
            return NO_DOC_FALLBACK_ANSWER;
        }
        if (persona == AiChatRequest.Persona.ADMIN) {
            return "open the Admin dashboard for the relevant queue and follow the documented checklist.";
        }
        return persona == AiChatRequest.Persona.LANDLORD
                ? "open your Landlord dashboard and follow the pending action shown there."
                : "open the listing and continue through the in-app flow.";
    }
}
