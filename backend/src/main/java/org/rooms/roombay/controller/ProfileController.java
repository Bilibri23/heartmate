package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.ProfileRequest;
import org.rooms.roombay.dto.response.ProfileResponse;
import org.rooms.roombay.security.AuthorizationPolicyService;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.FileUploadService;
import org.rooms.roombay.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Slf4j
@OpenAPIDefinition(tags = { @Tag(name = "Profile Management", description = "APIs for managing user profiles") })
public class ProfileController {

    private final ProfileService profileService;
    private final FileUploadService fileUploadService;
    private final AuthorizationPolicyService authorizationPolicyService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new profile", description = "Create a profile for a user (multipart)")
    public ResponseEntity<ProfileResponse> createProfileMultipart(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String[] languages,
            @RequestParam(required = false) String whatsappNumber,
            @RequestParam(required = false) String emergencyContactName,
            @RequestParam(required = false) String emergencyContactPhone,
            @RequestParam(required = false) String emergencyContactRelationship,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) UUID ignoredUserId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Creating profile for user: {}", userId);

        String profilePhotoUrl = null;
        if (file != null && !file.isEmpty()) {
            profilePhotoUrl = fileUploadService.uploadProfilePhoto(file);
        }

        ProfileRequest request = ProfileRequest.builder()
                .bio(bio)
                .city(city)
                .profilePhotoUrl(profilePhotoUrl)
                .languages(languages)
                .whatsappNumber(whatsappNumber)
                .emergencyContactName(emergencyContactName)
                .emergencyContactPhone(emergencyContactPhone)
                .emergencyContactRelationship(emergencyContactRelationship)
                .visibility(visibility)
                .build();

        ProfileResponse response = profileService.createProfile(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new profile (JSON)", description = "Create a profile using a JSON body and userId query parameter")
    public ResponseEntity<ProfileResponse> createProfileJson(
            @RequestParam(required = false) UUID ignoredUserId,
            @RequestBody @Valid ProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Creating profile for user: {} (JSON)", userId);
        ProfileResponse response = profileService.createProfile(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get profile by user ID", description = "Retrieve a user's profile by their user ID")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable UUID userId) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Fetching profile for user: {}", userId);
        ProfileResponse response = profileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update profile", description = "Update a user's profile (JSON)")
    public ResponseEntity<ProfileResponse> updateProfileJson(
            @PathVariable UUID userId,
            @RequestBody @Valid ProfileRequest request) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Updating profile for user: {} (JSON)", userId);
        log.info("Received request - bio: '{}', photoUrl: '{}'", request.getBio(), request.getProfilePhotoUrl());
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update profile", description = "Update a user's profile (Form Data)")
    public ResponseEntity<ProfileResponse> updateProfileForm(
            @PathVariable UUID userId,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String[] languages,
            @RequestParam(required = false) String whatsappNumber,
            @RequestParam(required = false) String emergencyContactName,
            @RequestParam(required = false) String emergencyContactPhone,
            @RequestParam(required = false) String emergencyContactRelationship,
            @RequestParam(required = false) String visibility) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Updating profile for user: {} (Form Data)", userId);
        
        // Upload profile photo if provided as file
        String profilePhotoUrl = null;
        if (file != null && !file.isEmpty()) {
            profilePhotoUrl = fileUploadService.uploadProfilePhoto(file);
        }
        
        // Build request from form parameters
        ProfileRequest request = ProfileRequest.builder()
                .bio(bio)
                .city(city)
                .profilePhotoUrl(profilePhotoUrl)
                .languages(languages)
                .whatsappNumber(whatsappNumber)
                .emergencyContactName(emergencyContactName)
                .emergencyContactPhone(emergencyContactPhone)
                .emergencyContactRelationship(emergencyContactRelationship)
                .visibility(visibility)
                .build();
        
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete profile", description = "Delete a user's profile")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID userId) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Deleting profile for user: {}", userId);
        profileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}
