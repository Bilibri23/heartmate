package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.AiFinetuneExampleRequest;
import org.rooms.roombay.dto.response.AiFinetuneEvalResponse;
import org.rooms.roombay.dto.response.AiFinetuneExampleResponse;
import org.rooms.roombay.dto.response.AiFinetuneStatsResponse;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.AiFinetuneDatasetService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/admin/finetune")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Finetune Dataset", description = "Admin-only curation of behaviour examples for finetuning")
public class AiFinetuneAdminController {

    private final AiFinetuneDatasetService datasetService;

    @GetMapping("/examples")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List active examples")
    public ResponseEntity<List<AiFinetuneExampleResponse>> listExamples() {
        return ResponseEntity.ok(datasetService.listActive());
    }

    @PostMapping("/examples")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create example", description = "Sanitizer is applied to system / user / context fields before persisting")
    public ResponseEntity<AiFinetuneExampleResponse> create(@Valid @RequestBody AiFinetuneExampleRequest req) {
        UUID admin = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(datasetService.create(req, admin));
    }

    @PutMapping("/examples/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update example")
    public ResponseEntity<AiFinetuneExampleResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AiFinetuneExampleRequest req) {
        return ResponseEntity.ok(datasetService.update(id, req));
    }

    @DeleteMapping("/examples/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate example", description = "Soft-delete: example is excluded from listings, exports, and eval")
    public ResponseEntity<Map<String, String>> deactivate(@PathVariable UUID id) {
        datasetService.deactivate(id);
        return ResponseEntity.ok(Map.of("status", "deactivated"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dataset stats", description = "Counts per kind plus a readiness hint for submitting a finetune job")
    public ResponseEntity<AiFinetuneStatsResponse> stats() {
        return ResponseEntity.ok(datasetService.stats());
    }

    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Seed bundled examples", description = "Idempotent load of the curated starter dataset (kind, userMessage) keys")
    public ResponseEntity<Map<String, Object>> seed() {
        int inserted = datasetService.seedFromBundledFile();
        return ResponseEntity.ok(Map.of("inserted", inserted));
    }

    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export JSONL", description = "OpenAI chat-completions finetune format")
    public ResponseEntity<byte[]> exportJsonl() {
        String body = datasetService.exportJsonl();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"roombay-finetune.jsonl\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @PostMapping("/eval")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Run evaluation", description = "Run the curated examples against the current model (use OPENAI_STRUCTURED_MODEL to test a finetuned head)")
    public ResponseEntity<AiFinetuneEvalResponse> evaluate(
            @RequestParam(defaultValue = "5") int limitPerKind) {
        return ResponseEntity.ok(datasetService.evaluate(limitPerKind));
    }
}
