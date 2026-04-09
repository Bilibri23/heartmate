package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.ai.AiModelRouter;
import org.rooms.roombay.ai.rag.AiRagRepository;
import org.rooms.roombay.dto.request.AiChatRequest;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.repository.AiChatLogRepository;
import org.rooms.roombay.repository.LeaseRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.rooms.roombay.security.SecurityUtils;
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
    private final AiRagRepository ragRepository;
    private final StudentVerificationRepository studentVerificationRepository;
    private final RoomApplicationRepository roomApplicationRepository;
    private final LeaseRepository leaseRepository;
    private final PropertyListingRepository propertyListingRepository;
    private final AiChatLogRepository chatLogRepository;

    public AiChatResponse chat(AiChatRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId(); // ensure authenticated
        // persona is provided by client; we still keep system prompt strict and role-agnostic.
        List<Double> qEmb = modelRouter.embed(request.getMessage());
        List<AiRagRepository.ChunkRow> chunks;
        try {
            chunks = ragRepository.topKSimilar(qEmb, 8);
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

        String system = buildSystemPrompt(request.getPersona());
        String userContext = buildUserContext(userId, request.getPersona());
        String raw = modelRouter.chat(system, request.getMessage(), contextChunks, userContext);
        ParsedAssistant parsed = parseAssistantOutput(raw);

        List<AiChatResponse.Citation> citations = chunks.stream().map(c -> AiChatResponse.Citation.builder()
                .chunkId(c.getId().toString())
                .source(c.getSource())
                .title(c.getTitle())
                .build()
        ).toList();

        AiChatResponse response = AiChatResponse.builder()
                .answer(parsed.answer)
                .citations(citations)
                .suggestedActions(parsed.actions)
                .build();

        chatLogRepository.insert(
                userId,
                request.getPersona().name(),
                request.getMessage(),
                response.getAnswer(),
                citations.stream().map(AiChatResponse.Citation::getChunkId).toList()
        );

        return response;
    }

    private String buildSystemPrompt(AiChatRequest.Persona persona) {
        String personaLine = persona == AiChatRequest.Persona.LANDLORD
                ? "You are helping a landlord user of RoomBay."
                : "You are helping a tenant user of RoomBay.";

        return String.join("\n",
                "You are RoomBay Assistant for the RoomBay platform.",
                personaLine,
                "",
                "Rules:",
                "- Only answer questions about RoomBay features, workflows, and policies.",
                "- Use the retrieved context chunks to ground your answer; do not invent features.",
                "- If the answer isn't in the context, say what you can infer and ask the user to check a specific page in the app.",
                "- Keep answers concise and actionable.",
                "- When referencing context, include chunkIds in parentheses like (chunkId: <id>).",
                "",
                "Output format:",
                "1) Provide the answer normally.",
                "2) Then on a new line write: SUGGESTED_ACTIONS_JSON: <json>",
                "Where <json> is a JSON array of up to 3 actions. Each action has:",
                "{ \"id\": \"string\", \"label\": \"string\", \"type\": \"NAVIGATE|COPY_TEXT\", \"actionUrl\": \"string?\", \"copyText\": \"string?\" }"
        );
    }

    private String buildUserContext(UUID userId, AiChatRequest.Persona persona) {
        List<String> lines = new ArrayList<>();
        String role = SecurityUtils.getCurrentUserRole();
        lines.add("role=" + role);
        lines.add("persona=" + persona.name());

        if (persona == AiChatRequest.Persona.TENANT) {
            var v = studentVerificationRepository.findByUserId(userId);
            lines.add("tenantVerificationStatus=" + v.map(ver -> ver.getStatus().name()).orElse("NONE"));
            lines.add("applicationsTotal=" + roomApplicationRepository.countByStudentId(userId));
            lines.add("applicationsPending=" + roomApplicationRepository.countByStudentIdAndStatus(userId, RoomApplication.Status.PENDING));
            lines.add("leasesTotal=" + leaseRepository.countByStudentId(userId));
        } else {
            lines.add("listingsTotal=" + propertyListingRepository.countByLandlordId(userId));
            lines.add("applicationsTotal=" + roomApplicationRepository.countByLandlordId(userId));
            lines.add("applicationsPending=" + roomApplicationRepository.countByLandlordIdAndStatus(userId, RoomApplication.Status.PENDING));
            lines.add("leasesTotal=" + leaseRepository.countByLandlordId(userId));
        }

        return String.join("\n", lines);
    }

    @lombok.Value
    private static class ParsedAssistant {
        String answer;
        List<AiChatResponse.SuggestedAction> actions;
    }

    private ParsedAssistant parseAssistantOutput(String raw) {
        if (raw == null) return new ParsedAssistant("", List.of());
        String marker = "SUGGESTED_ACTIONS_JSON:";
        int idx = raw.lastIndexOf(marker);
        if (idx < 0) {
            return new ParsedAssistant(raw.trim(), List.of());
        }
        String answer = raw.substring(0, idx).trim();
        String json = raw.substring(idx + marker.length()).trim();
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var listType = mapper.getTypeFactory().constructCollectionType(List.class, AiChatResponse.SuggestedAction.class);
            List<AiChatResponse.SuggestedAction> actions = mapper.readValue(json, listType);
            return new ParsedAssistant(answer, actions != null ? actions : List.of());
        } catch (Exception e) {
            return new ParsedAssistant(answer.isBlank() ? raw.trim() : answer, List.of());
        }
    }
}

