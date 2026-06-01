package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.ListingPreferencesRequest;
import org.rooms.roombay.dto.response.ListingPreferencesResponse;
import org.rooms.roombay.security.AuthorizationPolicyService;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.ListingPreferencesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/listing-preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Listing Preferences", description = "APIs for managing listing preferences")
public class ListingPreferencesController {
    
    private final ListingPreferencesService listingPreferencesService;
    private final AuthorizationPolicyService authorizationPolicyService;
    
    @PostMapping
    @Operation(summary = "Create listing preferences", description = "Create listing preferences for a user")
    public ResponseEntity<ListingPreferencesResponse> createPreferences(
            @Valid @RequestBody ListingPreferencesRequest request,
            @RequestParam(required = false) UUID ignoredUserId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Creating listing preferences for user: {}", userId);
        ListingPreferencesResponse response = listingPreferencesService.createPreferences(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get listing preferences by user ID", description = "Retrieve listing preferences for a user")
    public ResponseEntity<ListingPreferencesResponse> getPreferences(@PathVariable UUID userId) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Fetching listing preferences for user: {}", userId);
        ListingPreferencesResponse response = listingPreferencesService.getPreferences(userId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{userId}")
    @Operation(summary = "Update listing preferences", description = "Update listing preferences for a user")
    public ResponseEntity<ListingPreferencesResponse> updatePreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody ListingPreferencesRequest request) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Updating listing preferences for user: {}", userId);
        ListingPreferencesResponse response = listingPreferencesService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete listing preferences", description = "Delete listing preferences for a user")
    public ResponseEntity<Void> deletePreferences(@PathVariable UUID userId) {
        authorizationPolicyService.assertCurrentUserOrAdmin(userId);
        log.info("Deleting listing preferences for user: {}", userId);
        listingPreferencesService.deletePreferences(userId);
        return ResponseEntity.noContent().build();
    }
}
