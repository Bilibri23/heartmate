package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.AiChatRequest;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.rooms.roombay.dto.response.AiIngestResponse;
import org.rooms.roombay.service.AiAssistantService;
import org.rooms.roombay.service.AiIngestionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Assistant", description = "AI assistant (RAG) endpoints")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final AiIngestionService aiIngestionService;

    @Value("${roombay.ai.ingest-dev-key:}")
    private String ingestDevKey;

    @PostMapping("/chat")
    @Operation(summary = "Chat with assistant", description = "Tenant/Landlord assistant grounded in platform docs")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiAssistantService.chat(request));
    }

    @PostMapping("/admin/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ingest docs", description = "Admin-only: ingest docs/*.md into pgvector for RAG")
    public ResponseEntity<AiIngestResponse> ingest(@RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(aiIngestionService.ingestDocs(force));
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
}

