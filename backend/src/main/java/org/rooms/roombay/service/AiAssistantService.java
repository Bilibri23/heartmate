package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.ai.AiModelRouter;
import org.rooms.roombay.ai.rag.AiGraphRagService;
import org.rooms.roombay.ai.rag.AiRagRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantService {

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

    @Value("${roombay.ai.memory.max-turns:6}")
    private int memoryMaxTurns;

    public AiChatResponse chat(AiChatRequest request) {
        long started = System.currentTimeMillis();
        UUID userId = SecurityUtils.getCurrentUserId(); // ensure authenticated
        aiChatRateLimiter.checkAllowed(userId);
        String threadId = resolveThreadId(request.getThreadId());
        // persona is provided by client; we still keep system prompt strict and role-agnostic.
        List<Double> qEmb = modelRouter.embed(request.getMessage());
        List<AiRagRepository.ChunkRow> chunks;
        try {
            chunks = graphRagService.retrieve(qEmb, 8);
        } catch (Exception e) {
            // Keep assistant available even when vector index/schema has drift.
            log.warn("[AI] RAG retrieval failed, continuing without context: {}", e.getMessage());
            chunks = List.of();
        }

        List<Map<String, Object>> contextChunks = chunks.stream().map(c -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("chunkId", c.getId().toString());
            m.put("source", c.getSource());
            m.put("title", c.getTitle());
            m.put("text", c.getChunkText());
            return m;
        }).toList();

        boolean ragGrounded = !chunks.isEmpty();
        String system = buildSystemPrompt(request.getPersona(), ragGrounded);
        String userContext = buildUserContext(userId, request.getPersona());
        List<AiMemoryService.MemoryTurn> memoryTurns = aiMemoryService.loadRecentTurns(userId, threadId, memoryMaxTurns);
        String memoryContext = buildMemoryContext(memoryTurns);
        String fullUserContext = memoryContext.isBlank() ? userContext : userContext + "\n\n" + memoryContext;
        String raw = modelRouter.chat(system, request.getMessage(), contextChunks, fullUserContext);
        ParsedAssistant parsed = parseAssistantOutput(raw);

        List<AiChatResponse.Citation> citations = chunks.stream().map(c -> AiChatResponse.Citation.builder()
                .chunkId(c.getId().toString())
                .source(c.getSource())
                .title(c.getTitle())
                .build()
        ).toList();

        String safeAnswer = ensureNonEmptyAnswer(parsed.answer, ragGrounded);

        AiChatResponse response = AiChatResponse.builder()
                .answer(safeAnswer)
                .threadId(threadId)
                .citations(citations)
                .suggestedActions(parsed.actions)
                .ragGrounded(ragGrounded)
                .build();

        log.info("[AI] chat user={} ragChunks={} grounded={} graphRag={} ms={}",
                userId, chunks.size(), ragGrounded, graphRagService.isGraphRagEnabled(), System.currentTimeMillis() - started);

        chatLogRepository.insert(
                userId,
                request.getPersona().name(),
                request.getMessage(),
                response.getAnswer(),
                citations.stream().map(AiChatResponse.Citation::getChunkId).toList()
        );
        aiMemoryService.appendTurn(userId, threadId, request.getMessage(), response.getAnswer());

        return response;
    }

    private String buildSystemPrompt(AiChatRequest.Persona persona, boolean hasDocContext) {
        String personaLine = persona == AiChatRequest.Persona.LANDLORD
                ? "You are helping a landlord user of RoomBay."
                : "You are helping a tenant user of RoomBay.";

        String noKb = "";
        if (!hasDocContext) {
            noKb = String.join("\n",
                    "",
                    "Critical: No documentation chunks were retrieved from the RoomBay knowledge base for this question.",
                    "- Say clearly that you are answering without doc-grounded context.",
                    "- Do not invent RoomBay-specific screens, URLs, or policies.",
                    "- Suggest the user open Search, Profile, or Landlord dashboard as appropriate, or contact support.",
                    "- Keep the reply short."
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
                "- Use the retrieved context chunks and the User_context lines to ground your answer; do not invent features.",
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

    private String ensureNonEmptyAnswer(String answer, boolean ragGrounded) {
        if (answer != null && !answer.isBlank()) {
            return answer.trim();
        }
        if (ragGrounded) {
            return "I found related RoomBay information, but I could not format a full answer this time. Please ask again in one sentence, and I will answer clearly with the same sources.";
        }
        return "I could not generate a complete answer right now. Please rephrase your question, and if this continues, try again shortly.";
    }

    @lombok.Value
    private static class ParsedAssistant {
        String answer;
        List<AiChatResponse.SuggestedAction> actions;
    }

    private ParsedAssistant parseAssistantOutput(String raw) {
        if (raw == null) return new ParsedAssistant("", List.of());
        String trimmed = raw.trim();
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
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var listType = mapper.getTypeFactory().constructCollectionType(List.class, AiChatResponse.SuggestedAction.class);
            List<AiChatResponse.SuggestedAction> actions = mapper.readValue(json, listType);
            return new ParsedAssistant(answerPart, actions != null ? actions : List.of());
        } catch (Exception e) {
            log.debug("[AI] Could not parse suggested actions JSON: {}", e.getMessage());
            return new ParsedAssistant(answerPart.isBlank() ? sanitizeAnswerText(trimmed) : answerPart, List.of());
        }
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
}

