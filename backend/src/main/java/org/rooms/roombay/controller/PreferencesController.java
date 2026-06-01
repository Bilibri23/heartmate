package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.PreferencesRequest;
import org.rooms.roombay.dto.response.PreferencesResponse;
import org.rooms.roombay.security.AuthorizationPolicyService;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.PreferencesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Roommate Preferences", description = "APIs for managing roommate preferences")
public class PreferencesController {
    
    private final PreferencesService preferencesService;
    private final AuthorizationPolicyService authorizationPolicyService;
    
    @PostMapping
    @Operation(summary = "Create preferences", description = "Create roommate preferences for a user")
    public ResponseEntity<PreferencesResponse> createPreferences(
            @Valid @RequestBody PreferencesRequest request,
            @RequestParam(required = false) UUID ignoredUserId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Creating preferences for user: {}", userId);
        PreferencesResponse response = preferencesService.createPreferences(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get preferences by user ID", description = "Retrieve roommate preferences for a user")
    public ResponseEntity<PreferencesResponse> getPreferences(@PathVariable UUID userId) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Fetching preferences for user: {}", userId);
        PreferencesResponse response = preferencesService.getPreferences(userId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{userId}")
    @Operation(summary = "Update preferences", description = "Update roommate preferences for a user")
    public ResponseEntity<PreferencesResponse> updatePreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody PreferencesRequest request) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Updating preferences for user: {}", userId);
        PreferencesResponse response = preferencesService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete preferences", description = "Delete roommate preferences for a user")
    public ResponseEntity<Void> deletePreferences(@PathVariable UUID userId) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Deleting preferences for user: {}", userId);
        preferencesService.deletePreferences(userId);
        return ResponseEntity.noContent().build();
    }
}
