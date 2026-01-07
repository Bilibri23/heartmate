package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.response.DisputeResponse;
import org.rooms.roombuddy.entity.Dispute;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.DisputeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/disputes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Disputes", description = "Admin APIs for dispute resolution")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDisputeController {
    
    private final DisputeService disputeService;
    
    @GetMapping
    @Operation(summary = "Get all active disputes")
    public ResponseEntity<Page<DisputeResponse>> getActiveDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Admin {} fetching active disputes", SecurityUtils.getCurrentUserId());
        Pageable pageable = PageRequest.of(page, size, Sort.by("priority").descending().and(Sort.by("createdAt").ascending()));
        Page<DisputeResponse> disputes = disputeService.getActiveDisputes(pageable);
        return ResponseEntity.ok(disputes);
    }
    
    @GetMapping("/urgent")
    @Operation(summary = "Get urgent disputes")
    public ResponseEntity<List<DisputeResponse>> getUrgentDisputes() {
        log.info("Admin {} fetching urgent disputes", SecurityUtils.getCurrentUserId());
        List<DisputeResponse> disputes = disputeService.getUrgentDisputes();
        return ResponseEntity.ok(disputes);
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get disputes by status")
    public ResponseEntity<Page<DisputeResponse>> getDisputesByStatus(
            @PathVariable Dispute.DisputeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DisputeResponse> disputes = disputeService.getDisputesByStatus(status, pageable);
        return ResponseEntity.ok(disputes);
    }
    
    @PostMapping("/{disputeId}/assign")
    @Operation(summary = "Assign dispute to self")
    public ResponseEntity<DisputeResponse> assignDispute(@PathVariable UUID disputeId) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        log.info("Admin {} assigning dispute {} to self", adminId, disputeId);
        DisputeResponse dispute = disputeService.assignDispute(disputeId, adminId);
        return ResponseEntity.ok(dispute);
    }
    
    @PutMapping("/{disputeId}/status")
    @Operation(summary = "Update dispute status")
    public ResponseEntity<DisputeResponse> updateStatus(
            @PathVariable UUID disputeId,
            @RequestParam Dispute.DisputeStatus status) {
        
        UUID adminId = SecurityUtils.getCurrentUserId();
        log.info("Admin {} updating dispute {} status to {}", adminId, disputeId, status);
        DisputeResponse dispute = disputeService.updateDisputeStatus(disputeId, adminId, status);
        return ResponseEntity.ok(dispute);
    }
    
    @PostMapping("/{disputeId}/resolve")
    @Operation(summary = "Resolve dispute")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable UUID disputeId,
            @RequestParam Dispute.DisputeOutcome outcome,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Integer refundAmount) {
        
        UUID adminId = SecurityUtils.getCurrentUserId();
        log.info("Admin {} resolving dispute {} with outcome {}", adminId, disputeId, outcome);
        DisputeResponse dispute = disputeService.resolveDispute(disputeId, adminId, outcome, notes, refundAmount);
        return ResponseEntity.ok(dispute);
    }
}
