package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.ai.rag.AiGraphRagService;
import org.rooms.roombay.dto.request.AiActionExecuteRequest;
import org.rooms.roombay.dto.request.AiChatRequest;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.rooms.roombay.dto.response.AiGraphStatsResponse;
import org.rooms.roombay.dto.response.AiIngestResponse;
import org.rooms.roombay.repository.AiChatLogRepository;
import org.rooms.roombay.ai.rag.AiRagRepository;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.AiAssistantService;
import org.rooms.roombay.service.AiChatProgressListener;
import org.rooms.roombay.service.AiIngestionService;
import org.rooms.roombay.service.AiPrivilegedActionService;
import org.rooms.roombay.service.AnalyticsEventService;
import org.rooms.roombay.service.AuditLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Assistant", description = "AI assistant (RAG) endpoints")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final AiPrivilegedActionService aiPrivilegedActionService;
    private final AiIngestionService aiIngestionService;
    private final AiGraphRagService aiGraphRagService;
    private final AiChatLogRepository aiChatLogRepository;
    private final AiRagRepository aiRagRepository;
    private final AuditLogService auditLogService;
    private final AnalyticsEventService analyticsEventService;

    @Value("${roombay.ai.ingest-dev-key:}")
    private String ingestDevKey;

    @PostMapping("/chat")
    @Operation(summary = "Chat with assistant", description = "Tenant/Landlord assistant grounded in platform docs")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiAssistantService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream assistant progress", description = "SSE progress events with the same final payload as /api/ai/chat")
    public SseEmitter chatStream(@Valid @RequestBody AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(90_000L);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        java.util.UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        analyticsEventService.emit("ai_stream_started", userId, role, null,
                Map.of("persona", request.getPersona() == null ? "" : request.getPersona().name()));

        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.setContext(securityContext);
            try {
                AiChatResponse response = aiAssistantService.chat(request, new AiChatProgressListener() {
                    @Override
                    public void onRetrievalStarted() {
                        try {
                            sendEvent(emitter, "retrieval_started", Map.of(
                                    "label", "Searching RoomBay knowledge base..."
                            ));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onSourcesFound(int chunkCount) {
                        try {
                            sendEvent(emitter, "sources_found", Map.of(
                                    "label", chunkCount > 0
                                            ? "Found " + chunkCount + " relevant documents..."
                                            : "Checking role-safe sources..."
                            ));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onGenerationStarted() {
                        try {
                            sendEvent(emitter, "generation_started", Map.of(
                                    "label", "Generating grounded response..."
                            ));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onToken(String token) {
                        if (token == null || token.isEmpty()) {
                            return;
                        }
                        try {
                            sendEvent(emitter, "token", Map.of("token", token));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                sendEvent(emitter, "completed", response);
                analyticsEventService.emit("ai_stream_completed", userId, role, null,
                        Map.of(
                                "persona", request.getPersona() == null ? "" : request.getPersona().name(),
                                "ragGrounded", Boolean.TRUE.equals(response.getRagGrounded()),
                                "listingResultCount", response.getListingResults() == null ? 0 : response.getListingResults().size()
                        ));
                emitter.complete();
            } catch (Exception ex) {
                log.warn("[AI] stream failed: {}", ex.getMessage());
                try {
                    sendEvent(emitter, "error", Map.of(
                            "message", "Assistant streaming is unavailable. Falling back to the standard response."
                    ));
                } catch (Exception ignored) {
                    // The client may already have disconnected.
                }
                analyticsEventService.emit("ai_stream_failed", userId, role, null,
                        Map.of(
                                "persona", request.getPersona() == null ? "" : request.getPersona().name(),
                                "reason", ex.getClass().getSimpleName()
                        ));
                emitter.completeWithError(ex);
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        return emitter;
    }

    @PostMapping("/actions/execute")
    @Operation(summary = "Execute a confirmed AI action",
            description = "Runs a landlord/admin write action the user confirmed via the chat confirm-button. "
                    + "Role and ownership are re-validated server-side.")
    public ResponseEntity<AiChatResponse.ToolExecution> executeAction(@Valid @RequestBody AiActionExecuteRequest request) {
        AiChatResponse.ToolExecution result = aiPrivilegedActionService.execute(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUserRole(),
                request.getTool(),
                request.getParams());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/listing-events")
    @Operation(summary = "Track AI listing card event", description = "Authenticated best-effort analytics for RoomBay AI listing result actions")
    public ResponseEntity<Void> trackListingEvent(@RequestBody java.util.Map<String, Object> body) {
        String eventType = String.valueOf(body.getOrDefault("eventType", ""));
        if (!java.util.List.of(
                "ai_listing_result_clicked",
                "ai_listing_apply_clicked",
                "ai_listing_save_clicked").contains(eventType)) {
            return ResponseEntity.badRequest().build();
        }
        java.util.UUID listingId = null;
        Object rawListingId = body.get("listingId");
        if (rawListingId != null && !String.valueOf(rawListingId).isBlank()) {
            try {
                listingId = java.util.UUID.fromString(String.valueOf(rawListingId));
            } catch (IllegalArgumentException ignored) {
                return ResponseEntity.badRequest().build();
            }
        }
        analyticsEventService.emit(
                eventType,
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUserRole(),
                listingId,
                java.util.Map.of("source", "roombay_ai_listing_card"));
        return ResponseEntity.noContent().build();
    }

    private static void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
    }

    @PostMapping("/admin/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ingest docs", description = "Admin-only: ingest docs/*.md into pgvector for RAG")
    public ResponseEntity<AiIngestResponse> ingest(@RequestParam(defaultValue = "false") boolean force) {
        AiIngestResponse response = aiIngestionService.ingestDocs(force);
        auditLogService.logAdminAction(
                SecurityUtils.getCurrentUserId(),
                "AI_DOCS_INGEST",
                "AI",
                null,
                "AI docs ingest completed. force=" + force
                        + ", documents=" + response.getDocumentsProcessed()
                        + ", chunks=" + response.getChunksInserted()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/graph-stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "GraphRAG stats", description = "Counts for documents, chunks, entities, and edges")
    public ResponseEntity<AiGraphStatsResponse> graphStats() {
        return ResponseEntity.ok(aiGraphRagService.getGraphStats());
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "AI coverage analytics", description = "Admin-only: usage, grounding, role split, and source distribution for RoomBay AI")
    public ResponseEntity<java.util.Map<String, Object>> analytics() {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>(aiChatLogRepository.getCoverageStats());
        out.put("sourceDistribution", aiRagRepository.sourceDistribution());
        return ResponseEntity.ok(out);
    }

    /**
     * Local development only: no admin JWT required when {@code roombay.ai.ingest-dev-key} is set.
     * Send header {@code X-RoomBay-Ingest-Key} matching that value. Returns 404 when the key is not configured.
     */
    @PostMapping("/ingest-dev")
    @Operation(summary = "Ingest docs (dev key)", description = "Requires ROOMBAY_AI_INGEST_DEV_KEY and matching X-RoomBay-Ingest-Key header")
    public ResponseEntity<AiIngestResponse> ingestDev(
            @RequestHeader(value = "X-RoomBay-Ingest-Key", required = false) String key,
            @RequestParam(defaultValue = "false") boolean force) {
        if (ingestDevKey == null || ingestDevKey.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        if (key == null || !ingestDevKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("RAG doc ingest via ingest-dev (local key)");
        return ResponseEntity.ok(aiIngestionService.ingestDocs(force));
    }

    /**
     * Same dev-key gate as {@link #ingestDev}; use when you do not have an admin JWT for
     * {@link #graphStats()}.
     */
    @GetMapping("/graph-stats-dev")
    @Operation(summary = "GraphRAG stats (dev key)", description = "Requires ROOMBAY_AI_INGEST_DEV_KEY and matching X-RoomBay-Ingest-Key header")
    public ResponseEntity<AiGraphStatsResponse> graphStatsDev(
            @RequestHeader(value = "X-RoomBay-Ingest-Key", required = false) String key) {
        if (ingestDevKey == null || ingestDevKey.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        if (key == null || !ingestDevKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(aiGraphRagService.getGraphStats());
    }
}
