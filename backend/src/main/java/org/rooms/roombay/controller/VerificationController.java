package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.VerificationRequest;
import org.rooms.roombay.dto.response.VerificationResponse;
import org.rooms.roombay.security.AuthorizationPolicyService;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.VerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/verifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Verification", description = "APIs for student verification")
public class VerificationController {
    
    private final VerificationService verificationService;
    private final AuthorizationPolicyService authorizationPolicyService;
    
    @PostMapping
    @Operation(summary = "Submit verification request", description = "Submit a student verification request")
    public ResponseEntity<VerificationResponse> submitVerification(
            @Valid @RequestBody VerificationRequest request,
            @RequestParam(required = false) UUID ignoredUserId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Submitting verification request for user: {}", userId);
        VerificationResponse response = verificationService.submitVerification(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user's verification status", description = "Returns status NONE when no verification has been submitted yet")
    public ResponseEntity<VerificationResponse> getMyVerification() {
        UUID userId = SecurityUtils.getCurrentUserId();
        VerificationResponse response = verificationService.getVerificationOrNone(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get verification by user ID", description = "Retrieve verification status for a user")
    public ResponseEntity<VerificationResponse> getVerification(@PathVariable UUID userId) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Fetching verification for user: {}", userId);
        VerificationResponse response = verificationService.getVerification(userId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{userId}")
    @Operation(summary = "Update verification", description = "Update a verification request (only if PENDING or REJECTED)")
    public ResponseEntity<VerificationResponse> updateVerification(
            @PathVariable UUID userId,
            @Valid @RequestBody VerificationRequest request) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Updating verification for user: {}", userId);
        VerificationResponse response = verificationService.updateVerification(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete verification", description = "Delete a verification request")
    public ResponseEntity<Void> deleteVerification(@PathVariable UUID userId) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Deleting verification for user: {}", userId);
        verificationService.deleteVerification(userId);
        return ResponseEntity.noContent().build();
    }
}
