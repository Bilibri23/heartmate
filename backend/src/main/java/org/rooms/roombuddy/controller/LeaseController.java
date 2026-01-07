package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.LeaseRequest;
import org.rooms.roombuddy.dto.response.LeaseResponse;
import org.rooms.roombuddy.entity.Lease;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.LeaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/leases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Leases", description = "Lease management APIs")
@SecurityRequirement(name = "bearerAuth")
public class LeaseController {
    
    private final LeaseService leaseService;
    
    @PostMapping
    @Operation(summary = "Create lease from accepted application", description = "Landlord creates a lease after accepting an application")
    public ResponseEntity<LeaseResponse> createLease(@Valid @RequestBody LeaseRequest request) {
        UUID landlordId = SecurityUtils.getCurrentUserId();
        log.info("Landlord {} creating lease from application {}", landlordId, request.getApplicationId());
        LeaseResponse lease = leaseService.createLeaseFromApplication(landlordId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(lease);
    }
    
    @GetMapping("/{leaseId}")
    @Operation(summary = "Get lease by ID")
    public ResponseEntity<LeaseResponse> getLeaseById(@PathVariable UUID leaseId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        LeaseResponse lease = leaseService.getLeaseById(leaseId, userId);
        return ResponseEntity.ok(lease);
    }
    
    @GetMapping("/reference/{referenceCode}")
    @Operation(summary = "Get lease by reference code")
    public ResponseEntity<LeaseResponse> getLeaseByReference(@PathVariable String referenceCode) {
        UUID userId = SecurityUtils.getCurrentUserId();
        LeaseResponse lease = leaseService.getLeaseByReference(referenceCode, userId);
        return ResponseEntity.ok(lease);
    }
    
    @GetMapping("/my")
    @Operation(summary = "Get my leases", description = "Get all leases where user is student or landlord")
    public ResponseEntity<Page<LeaseResponse>> getMyLeases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<LeaseResponse> leases = leaseService.getMyLeases(userId, pageable);
        return ResponseEntity.ok(leases);
    }
    
    @GetMapping("/as-student")
    @Operation(summary = "Get leases as student")
    public ResponseEntity<Page<LeaseResponse>> getLeasesAsStudent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<LeaseResponse> leases = leaseService.getLeasesAsStudent(userId, pageable);
        return ResponseEntity.ok(leases);
    }
    
    @GetMapping("/as-landlord")
    @Operation(summary = "Get leases as landlord")
    public ResponseEntity<Page<LeaseResponse>> getLeasesAsLandlord(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<LeaseResponse> leases = leaseService.getLeasesAsLandlord(userId, pageable);
        return ResponseEntity.ok(leases);
    }
    
    @PostMapping("/{leaseId}/accept-terms")
    @Operation(summary = "Accept lease terms", description = "Student or landlord accepts the lease terms")
    public ResponseEntity<LeaseResponse> acceptTerms(@PathVariable UUID leaseId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} accepting terms for lease {}", userId, leaseId);
        LeaseResponse lease = leaseService.acceptTerms(leaseId, userId);
        return ResponseEntity.ok(lease);
    }
    
    @PostMapping("/{leaseId}/terminate")
    @Operation(summary = "Terminate lease", description = "Terminate an active lease")
    public ResponseEntity<LeaseResponse> terminateLease(
            @PathVariable UUID leaseId,
            @RequestParam Lease.TerminationReason reason,
            @RequestParam(required = false) String notes) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} terminating lease {} with reason: {}", userId, leaseId, reason);
        LeaseResponse lease = leaseService.terminateLease(leaseId, userId, reason, notes);
        return ResponseEntity.ok(lease);
    }
    
    @PostMapping("/{leaseId}/complete")
    @Operation(summary = "Complete lease", description = "Landlord marks lease as completed")
    public ResponseEntity<LeaseResponse> completeLease(@PathVariable UUID leaseId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Landlord {} completing lease {}", userId, leaseId);
        LeaseResponse lease = leaseService.completeLease(leaseId, userId);
        return ResponseEntity.ok(lease);
    }
}
