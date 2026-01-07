package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.DisputeRequest;
import org.rooms.roombuddy.dto.response.DisputeResponse;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.DisputeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Disputes", description = "Dispute resolution APIs")
@SecurityRequirement(name = "bearerAuth")
public class DisputeController {
    
    private final DisputeService disputeService;
    
    @PostMapping
    @Operation(summary = "Create dispute", description = "Open a dispute for a lease")
    public ResponseEntity<DisputeResponse> createDispute(@Valid @RequestBody DisputeRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} creating dispute for lease {}", userId, request.getLeaseId());
        DisputeResponse dispute = disputeService.createDispute(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dispute);
    }
    
    @GetMapping("/{disputeId}")
    @Operation(summary = "Get dispute by ID")
    public ResponseEntity<DisputeResponse> getDisputeById(@PathVariable UUID disputeId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        DisputeResponse dispute = disputeService.getDisputeById(disputeId, userId);
        return ResponseEntity.ok(dispute);
    }
    
    @GetMapping("/my")
    @Operation(summary = "Get my disputes", description = "Get disputes I opened or are against me")
    public ResponseEntity<Page<DisputeResponse>> getMyDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DisputeResponse> disputes = disputeService.getMyDisputes(userId, pageable);
        return ResponseEntity.ok(disputes);
    }
    
    @PostMapping("/{disputeId}/evidence")
    @Operation(summary = "Add evidence", description = "Add evidence to a dispute")
    public ResponseEntity<DisputeResponse> addEvidence(
            @PathVariable UUID disputeId,
            @RequestBody List<String> evidenceUrls) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} adding evidence to dispute {}", userId, disputeId);
        DisputeResponse dispute = disputeService.addEvidence(disputeId, userId, evidenceUrls);
        return ResponseEntity.ok(dispute);
    }
}
