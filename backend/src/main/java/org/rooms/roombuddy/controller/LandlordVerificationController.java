package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.BusinessVerificationRequest;
import org.rooms.roombuddy.dto.request.LandlordVerificationRequest;
import org.rooms.roombuddy.dto.request.PropertyVerificationRequest;
import org.rooms.roombuddy.dto.response.LandlordVerificationResponse;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.LandlordVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for landlord KYC verification.
 * Handles identity, business, and property verification submissions.
 */
@RestController
@RequestMapping("/api/landlord-verifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Landlord Verification", description = "APIs for landlord KYC verification")
@SecurityRequirement(name = "bearerAuth")
public class LandlordVerificationController {
    
    private final LandlordVerificationService verificationService;
    
    // ==================== IDENTITY VERIFICATION ====================
    
    @PostMapping("/identity")
    @Operation(summary = "Submit identity verification", 
               description = "Submit identity documents for KYC verification (Landlord only)")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<LandlordVerificationResponse> submitIdentityVerification(
            @Valid @RequestBody LandlordVerificationRequest request) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Landlord {} submitting identity verification", userId);
        
        LandlordVerificationResponse response = verificationService.submitIdentityVerification(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    // ==================== BUSINESS VERIFICATION ====================
    
    @PostMapping("/business")
    @Operation(summary = "Submit business verification", 
               description = "Submit business registration documents (optional, for property managers)")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<LandlordVerificationResponse> submitBusinessVerification(
            @Valid @RequestBody BusinessVerificationRequest request) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Landlord {} submitting business verification", userId);
        
        LandlordVerificationResponse response = verificationService.submitBusinessVerification(userId, request);
        return ResponseEntity.ok(response);
    }
    
    // ==================== PROPERTY VERIFICATION ====================
    
    @PostMapping("/property")
    @Operation(summary = "Submit property verification", 
               description = "Submit property ownership documents")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<LandlordVerificationResponse> submitPropertyVerification(
            @Valid @RequestBody PropertyVerificationRequest request) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Landlord {} submitting property verification", userId);
        
        LandlordVerificationResponse response = verificationService.submitPropertyVerification(userId, request);
        return ResponseEntity.ok(response);
    }
    
    // ==================== GET VERIFICATION STATUS ====================
    
    @GetMapping("/me")
    @Operation(summary = "Get my verification status", 
               description = "Get current landlord's verification status")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<LandlordVerificationResponse> getMyVerification() {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Landlord {} fetching their verification status", userId);
        
        LandlordVerificationResponse response = verificationService.getVerification(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get landlord verification by user ID", 
               description = "Get verification status for a specific landlord (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LandlordVerificationResponse> getVerificationByUserId(@PathVariable UUID userId) {
        log.info("Admin fetching verification for landlord: {}", userId);
        
        LandlordVerificationResponse response = verificationService.getVerification(userId);
        return ResponseEntity.ok(response);
    }
}
