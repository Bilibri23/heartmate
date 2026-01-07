package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.LandlordVerificationApprovalRequest;
import org.rooms.roombuddy.dto.request.ListingApprovalRequest;
import org.rooms.roombuddy.dto.request.VerificationApprovalRequest;
import org.rooms.roombuddy.dto.response.LandlordVerificationResponse;
import org.rooms.roombuddy.dto.response.ListingResponse;
import org.rooms.roombuddy.dto.response.StatisticsResponse;
import org.rooms.roombuddy.dto.response.VerificationResponse;
import org.rooms.roombuddy.entity.AuditLog;
import org.rooms.roombuddy.entity.StudentVerification;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.AuditLogService;
import org.rooms.roombuddy.service.LandlordVerificationService;
import org.rooms.roombuddy.service.ListingService;
import org.rooms.roombuddy.service.StatisticsService;
import org.rooms.roombuddy.service.VerificationService;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin APIs for managing verifications and listings")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    
    private final VerificationService verificationService;
    private final LandlordVerificationService landlordVerificationService;
    private final ListingService listingService;
    private final StatisticsService statisticsService;
    private final AuditLogService auditLogService;
    
    @GetMapping("/statistics")
    @Operation(summary = "Get platform statistics", description = "Get platform statistics (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatisticsResponse> getStatistics() {
        log.info("Admin {} fetching platform statistics", SecurityUtils.getCurrentUserId());
        StatisticsResponse statistics = statisticsService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/verifications/pending")
    @Operation(summary = "Get pending verifications", description = "Get all pending verification requests (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VerificationResponse>> getPendingVerifications() {
        log.info("Admin {} fetching pending verifications", SecurityUtils.getCurrentUserId());
        List<VerificationResponse> verifications = verificationService.getPendingVerifications();
        return ResponseEntity.ok(verifications);
    }
    
    @GetMapping("/verifications")
    @Operation(summary = "Get all verifications", description = "Get all verification requests with optional status filter (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VerificationResponse>> getAllVerifications(
            @RequestParam(required = false) String status) {
        log.info("Admin {} fetching all verifications with status: {}", SecurityUtils.getCurrentUserId(), status);
        
        StudentVerification.Status verificationStatus = null;
        if (status != null) {
            try {
                verificationStatus = StudentVerification.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        List<VerificationResponse> verifications = verificationService.getAllVerifications(verificationStatus);
        return ResponseEntity.ok(verifications);
    }
    
    @PostMapping("/verifications/{verificationId}/approve")
    @Operation(summary = "Approve or reject verification", description = "Approve or reject a verification request (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VerificationResponse> approveOrRejectVerification(
            @PathVariable UUID verificationId,
            @Valid @RequestBody VerificationApprovalRequest request) {
        log.info("Admin {} approving/rejecting verification: {}", SecurityUtils.getCurrentUserId(), verificationId);
        
        UUID adminId = SecurityUtils.getCurrentUserId();
        VerificationResponse response = verificationService.approveOrRejectVerification(verificationId, adminId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/listings/pending")
    @Operation(summary = "Get pending listings", description = "Get all pending listings awaiting approval (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ListingResponse>> getPendingListings() {
        log.info("Admin {} fetching pending listings", SecurityUtils.getCurrentUserId());
        List<ListingResponse> listings = listingService.getPendingListings();
        return ResponseEntity.ok(listings);
    }
    
    @PostMapping("/listings/{listingId}/approve")
    @Operation(summary = "Approve or reject listing", description = "Approve or reject a property listing (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ListingResponse> approveOrRejectListing(
            @PathVariable UUID listingId,
            @Valid @RequestBody ListingApprovalRequest request) {
        log.info("Admin {} approving/rejecting listing: {}", SecurityUtils.getCurrentUserId(), listingId);
        
        UUID adminId = SecurityUtils.getCurrentUserId();
        ListingResponse response = listingService.approveOrRejectListing(listingId, adminId, request);
        return ResponseEntity.ok(response);
    }
    
    // ==================== LANDLORD VERIFICATION MANAGEMENT ====================
    
    @GetMapping("/landlord-verifications/pending")
    @Operation(summary = "Get pending landlord verifications", 
               description = "Get all pending landlord KYC verification requests (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LandlordVerificationResponse>> getPendingLandlordVerifications() {
        log.info("Admin {} fetching pending landlord verifications", SecurityUtils.getCurrentUserId());
        List<LandlordVerificationResponse> verifications = landlordVerificationService.getPendingVerifications();
        return ResponseEntity.ok(verifications);
    }
    
    @GetMapping("/landlord-verifications")
    @Operation(summary = "Get all landlord verifications", 
               description = "Get all landlord verifications with pagination (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<LandlordVerificationResponse>> getAllLandlordVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin {} fetching all landlord verifications", SecurityUtils.getCurrentUserId());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LandlordVerificationResponse> verifications = landlordVerificationService.getPendingVerifications(pageable);
        return ResponseEntity.ok(verifications);
    }
    
    @GetMapping("/landlord-verifications/{verificationId}")
    @Operation(summary = "Get landlord verification by ID", 
               description = "Get a specific landlord verification (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LandlordVerificationResponse> getLandlordVerificationById(
            @PathVariable UUID verificationId) {
        log.info("Admin {} fetching landlord verification: {}", SecurityUtils.getCurrentUserId(), verificationId);
        LandlordVerificationResponse response = landlordVerificationService.getVerificationById(verificationId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/landlord-verifications/{verificationId}/approve")
    @Operation(summary = "Approve or reject landlord verification", 
               description = "Approve or reject a landlord KYC verification (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LandlordVerificationResponse> approveOrRejectLandlordVerification(
            @PathVariable UUID verificationId,
            @Valid @RequestBody LandlordVerificationApprovalRequest request) {
        log.info("Admin {} processing landlord verification: {} - type: {}, status: {}", 
                SecurityUtils.getCurrentUserId(), verificationId, 
                request.getVerificationType(), request.getStatus());
        
        UUID adminId = SecurityUtils.getCurrentUserId();
        LandlordVerificationResponse response = landlordVerificationService.approveOrRejectVerification(
                verificationId, adminId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/landlord-verifications/trusted")
    @Operation(summary = "Get trusted landlords", 
               description = "Get all landlords with trusted status (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LandlordVerificationResponse>> getTrustedLandlords() {
        log.info("Admin {} fetching trusted landlords", SecurityUtils.getCurrentUserId());
        List<LandlordVerificationResponse> landlords = landlordVerificationService.getTrustedLandlords();
        return ResponseEntity.ok(landlords);
    }
    
    @GetMapping("/landlord-verifications/reported")
    @Operation(summary = "Get reported landlords", 
               description = "Get landlords with reports (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LandlordVerificationResponse>> getReportedLandlords() {
        log.info("Admin {} fetching reported landlords", SecurityUtils.getCurrentUserId());
        List<LandlordVerificationResponse> landlords = landlordVerificationService.getReportedLandlords();
        return ResponseEntity.ok(landlords);
    }
    
    // ==================== AUDIT LOG MANAGEMENT ====================
    
    @GetMapping("/audit-logs")
    @Operation(summary = "Get admin audit logs", 
               description = "Get all admin actions for accountability (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("Admin {} fetching audit logs", SecurityUtils.getCurrentUserId());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> auditLogs = auditLogService.getAdminActions(pageable);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/audit-logs/user/{userId}")
    @Operation(summary = "Get audit logs by user", 
               description = "Get all actions by a specific user (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("Admin {} fetching audit logs for user: {}", SecurityUtils.getCurrentUserId(), userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> auditLogs = auditLogService.getActionsByUser(userId, pageable);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/audit-logs/entity/{entityType}/{entityId}")
    @Operation(summary = "Get entity history", 
               description = "Get all actions on a specific entity (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        log.info("Admin {} fetching history for {}: {}", SecurityUtils.getCurrentUserId(), entityType, entityId);
        List<AuditLog> history = auditLogService.getEntityHistory(entityType, entityId);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/audit-logs/search")
    @Operation(summary = "Search audit logs", 
               description = "Search audit logs by action keyword (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> searchAuditLogs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("Admin {} searching audit logs for: {}", SecurityUtils.getCurrentUserId(), keyword);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> auditLogs = auditLogService.searchActions(keyword, pageable);
        return ResponseEntity.ok(auditLogs);
    }
}

