package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.RealtorProfileRequest;
import org.rooms.roombay.dto.request.RealtorVerificationDocumentRequest;
import org.rooms.roombay.dto.response.RealtorProfileResponse;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.RealtorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Realtor onboarding and self-service (RoomBay 2.0 — verified realtor role). */
@RestController
@RequestMapping("/api/realtors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Realtors", description = "Realtor onboarding, profile, and verification documents")
public class RealtorController {

    private final RealtorService realtorService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('REALTOR')")
    @Operation(summary = "Create realtor profile", description = "Complete realtor onboarding for the current REALTOR user")
    public ResponseEntity<RealtorProfileResponse> register(@Valid @RequestBody RealtorProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(realtorService.registerProfile(userId, request), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('REALTOR')")
    @Operation(summary = "Get my realtor profile")
    public ResponseEntity<RealtorProfileResponse> getMe() {
        return ResponseEntity.ok(realtorService.getMyProfile(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('REALTOR')")
    @Operation(summary = "Update my realtor profile")
    public ResponseEntity<RealtorProfileResponse> updateMe(@Valid @RequestBody RealtorProfileRequest request) {
        return ResponseEntity.ok(realtorService.updateProfile(SecurityUtils.getCurrentUserId(), request));
    }

    @PostMapping("/verification-documents")
    @PreAuthorize("hasRole('REALTOR')")
    @Operation(summary = "Submit a verification document", description = "Upload a KYC document for admin review")
    public ResponseEntity<RealtorProfileResponse> addDocument(
            @Valid @RequestBody RealtorVerificationDocumentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(realtorService.addDocument(userId, request), HttpStatus.CREATED);
    }
}
