package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.ListingPreferencesRequest;
import org.rooms.roombuddy.dto.response.ListingPreferencesResponse;
import org.rooms.roombuddy.service.ListingPreferencesService;
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
    
    @PostMapping
    @Operation(summary = "Create listing preferences", description = "Create listing preferences for a user")
    public ResponseEntity<ListingPreferencesResponse> createPreferences(
            @Valid @RequestBody ListingPreferencesRequest request,
            @RequestParam UUID userId) {
        log.info("Creating listing preferences for user: {}", userId);
        ListingPreferencesResponse response = listingPreferencesService.createPreferences(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get listing preferences by user ID", description = "Retrieve listing preferences for a user")
    public ResponseEntity<ListingPreferencesResponse> getPreferences(@PathVariable UUID userId) {
        log.info("Fetching listing preferences for user: {}", userId);
        ListingPreferencesResponse response = listingPreferencesService.getPreferences(userId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{userId}")
    @Operation(summary = "Update listing preferences", description = "Update listing preferences for a user")
    public ResponseEntity<ListingPreferencesResponse> updatePreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody ListingPreferencesRequest request) {
        log.info("Updating listing preferences for user: {}", userId);
        ListingPreferencesResponse response = listingPreferencesService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete listing preferences", description = "Delete listing preferences for a user")
    public ResponseEntity<Void> deletePreferences(@PathVariable UUID userId) {
        log.info("Deleting listing preferences for user: {}", userId);
        listingPreferencesService.deletePreferences(userId);
        return ResponseEntity.noContent().build();
    }
}
