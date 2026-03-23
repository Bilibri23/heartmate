package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.AiChatRequest;
import org.rooms.roombuddy.dto.response.AiChatResponse;
import org.rooms.roombuddy.dto.response.AiIngestResponse;
import org.rooms.roombuddy.service.AiAssistantService;
import org.rooms.roombuddy.service.AiIngestionService;
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
}

