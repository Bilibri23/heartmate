package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.ai.AiModelRouter;
import org.rooms.roombay.ai.rag.AiGraphRagService;
import org.rooms.roombay.ai.rag.AiRagRepository;
import org.rooms.roombay.ai.safety.AiContextSanitizer;
import org.rooms.roombay.ai.safety.AiRetrievalPolicy;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.rooms.roombay.entity.ListingPreferences;
import org.rooms.roombay.repository.ListingPreferencesRepository;
import org.rooms.roombay.service.AiAgentToolService;
import org.rooms.roombay.service.AiCopilotToolService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal, token-gated tool endpoints for the LangGraph orchestrator sidecar.
 *
 * <p>These are NOT public API and are not JWT-authenticated; the shared internal
 * token is the trust anchor (see SecurityConfig permit + token check here). Each
 * call carries {@code userId} / {@code role} (originally derived by Spring from the
 * caller's JWT and passed through the sidecar), and role is re-applied here:
 * retrieval is role-filtered, listing search is tenant-scoped. Write tools are
 * allowlisted and executed via {@link AiAgentToolService} with the same role checks
 * as the public API.
 */
@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
@Slf4j
@Hidden
public class AiInternalToolController {

    private final AiModelRouter modelRouter;
    private final AiGraphRagService graphRagService;
    private final AiRetrievalPolicy retrievalPolicy;
    private final AiContextSanitizer contextSanitizer;
    private final AiCopilotToolService aiCopilotToolService;
    private final AiAgentToolService aiAgentToolService;
    private final ListingPreferencesRepository listingPreferencesRepository;

    @Value("${roombay.ai.internal.token:}")
    private String internalToken;

    @Value("${roombay.ai.chat-retrieval-k:6}")
    private int chatRetrievalK;

    @PostMapping("/retrieve")
    public ResponseEntity<Map<String, Object>> retrieve(
            @RequestHeader(value = "X-RoomBay-Internal-Token", required = false) String token,
            @RequestBody Map<String, Object> body) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String message = str(body.get("message"));
        String role = roleOrDefault(body.get("role"));

        List<AiGraphRagService.ScoredChunk> scored;
        try {
            List<Double> qEmb = modelRouter.embed(message);
            scored = graphRagService.retrieveWithScores(qEmb, chatRetrievalK);
        } catch (Exception ex) {
            log.warn("[AI][internal] retrieve embedding/retrieval failed: {}", ex.getMessage());
            scored = List.of();
        }
        List<AiRagRepository.ChunkRow> chunks = scored.stream()
                .map(AiGraphRagService.ScoredChunk::getChunk)
                .toList();
        chunks = retrievalPolicy.filter(chunks, role);

        Map<UUID, Double> scoreByChunkId = new HashMap<>();
        for (AiGraphRagService.ScoredChunk sc : scored) {
            if (sc.getChunk() != null && sc.getChunk().getId() != null) {
                scoreByChunkId.put(sc.getChunk().getId(), sc.getScore());
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (AiRagRepository.ChunkRow c : chunks) {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("chunkId", c.getId() == null ? null : c.getId().toString());
            m.put("source", c.getSource());
            m.put("title", c.getTitle());
            m.put("text", contextSanitizer.sanitize(c.getChunkText()));
            double score = c.getId() == null ? 0.0 : scoreByChunkId.getOrDefault(c.getId(), 0.0);
            m.put("score", score);
            out.add(m);
        }
        return ResponseEntity.ok(Map.of("chunks", out));
    }

    @PostMapping("/listings/search")
    public ResponseEntity<Map<String, Object>> searchListings(
            @RequestHeader(value = "X-RoomBay-Internal-Token", required = false) String token,
            @RequestBody Map<String, Object> body) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = uuid(body.get("userId"));
        String role = roleOrDefault(body.get("role"));
        Map<String, Object> filters = asMap(body.get("filters"));

        String query = firstNonBlank(str(filters.get("query")), str(filters.get("neighborhood")), str(filters.get("city")));
        String city = firstNonBlank(str(filters.get("city")), str(filters.get("neighborhood")));

        List<AiChatResponse.ListingResult> listings = aiCopilotToolService.searchListingsForOrchestrator(
                userId,
                role,
                query,
                city,
                str(filters.get("propertyType")),
                integer(filters.get("maxBudget")),
                str(filters.get("listingPurpose")),
                str(filters.get("stayType")),
                str(filters.get("furnishingStatus")),
                bool(filters.get("sharedAccommodation")));

        return ResponseEntity.ok(Map.of("listings", listings));
    }

    @PostMapping("/preferences")
    public ResponseEntity<Map<String, Object>> preferences(
            @RequestHeader(value = "X-RoomBay-Internal-Token", required = false) String token,
            @RequestBody Map<String, Object> body) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = uuid(body.get("userId"));
        if (userId == null) {
            return ResponseEntity.ok(Map.of());
        }
        Optional<ListingPreferences> prefs = listingPreferencesRepository.findByUserId(userId);
        if (prefs.isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }
        ListingPreferences p = prefs.get();
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("minBudget", p.getMinBudget());
        out.put("maxBudget", p.getMaxBudget());
        out.put("preferredLocations", p.getPreferredLocations() == null ? List.of() : p.getPreferredLocations());
        out.put("propertyTypes", p.getPropertyTypes() == null ? List.of() : p.getPropertyTypes());
        return ResponseEntity.ok(out);
    }

    @PostMapping("/tools/execute")
    public ResponseEntity<Map<String, Object>> executeTool(
            @RequestHeader(value = "X-RoomBay-Internal-Token", required = false) String token,
            @RequestBody Map<String, Object> body) {
        if (!authorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = uuid(body.get("userId"));
        String role = roleOrDefault(body.get("role"));
        String tool = str(body.get("tool"));
        Map<String, Object> params = asMap(body.get("params"));
        String requestId = str(body.get("requestId"));

        if (userId == null || tool == null || tool.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "userId and tool are required"));
        }

        AiAgentToolService.ToolExecutionResult result = aiAgentToolService.execute(
                userId, role, tool.trim().toUpperCase(java.util.Locale.ROOT), params, requestId);
        return ResponseEntity.ok(Map.of(
                "tool", result.tool(),
                "success", result.success(),
                "message", result.message(),
                "entityId", result.entityId() == null ? "" : result.entityId()));
    }

    // --- helpers ---

    private boolean authorized(String token) {
        // Closed by default: a blank configured token rejects all callers.
        if (internalToken == null || internalToken.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                internalToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    private static String roleOrDefault(Object role) {
        String r = str(role);
        return (r == null || r.isBlank()) ? "STUDENT" : r;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static Integer integer(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean bool(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        return Boolean.valueOf(String.valueOf(o));
    }

    private static UUID uuid(Object o) {
        if (o == null) return null;
        try {
            return UUID.fromString(String.valueOf(o).trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
