package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.SavedSearchRequest;
import org.rooms.roombay.dto.response.SavedSearchResponse;
import org.rooms.roombay.service.SavedSearchService;
import org.rooms.roombay.util.InputSanitizer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/saved-searches")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Saved Searches", description = "Saved search management endpoints")
public class SavedSearchController {
    
    private final SavedSearchService savedSearchService;
    private final InputSanitizer inputSanitizer;
    
    @PostMapping
    @Operation(summary = "Create saved search", description = "Save search criteria for future use and notifications")
    public ResponseEntity<SavedSearchResponse> createSavedSearch(
            @RequestParam UUID userId,
            @Valid @RequestBody SavedSearchRequest request) {
        log.info("Creating saved search for user: {}", userId);
        
        // SECURITY: Sanitize all inputs
        try {
            sanitizeSavedSearchRequest(request);
        } catch (SecurityException e) {
            log.warn("Security violation in saved search creation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        
        SavedSearchResponse response = savedSearchService.createSavedSearch(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's saved searches", description = "Get all saved searches for a user")
    public ResponseEntity<List<SavedSearchResponse>> getUserSavedSearches(@PathVariable UUID userId) {
        log.info("Getting saved searches for user: {}", userId);
        List<SavedSearchResponse> searches = savedSearchService.getUserSavedSearches(userId);
        return ResponseEntity.ok(searches);
    }
    
    @PutMapping("/{searchId}")
    @Operation(summary = "Update saved search", description = "Update an existing saved search")
    public ResponseEntity<SavedSearchResponse> updateSavedSearch(
            @PathVariable UUID searchId,
            @RequestParam UUID userId,
            @Valid @RequestBody SavedSearchRequest request) {
        log.info("Updating saved search: {} for user: {}", searchId, userId);
        
        // SECURITY: Sanitize all inputs
        try {
            sanitizeSavedSearchRequest(request);
        } catch (SecurityException e) {
            log.warn("Security violation in saved search update: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        
        SavedSearchResponse response = savedSearchService.updateSavedSearch(searchId, userId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{searchId}")
    @Operation(summary = "Delete saved search", description = "Delete a saved search")
    public ResponseEntity<Void> deleteSavedSearch(
            @PathVariable UUID searchId,
            @RequestParam UUID userId) {
        log.info("Deleting saved search: {} for user: {}", searchId, userId);
        savedSearchService.deleteSavedSearch(searchId, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Helper method to sanitize all fields in a SavedSearchRequest
     */
    private void sanitizeSavedSearchRequest(SavedSearchRequest request) {
        request.setName(inputSanitizer.sanitizeString(request.getName()));
        request.setQuery(inputSanitizer.sanitizeString(request.getQuery()));
        request.setCity(inputSanitizer.sanitizeCity(request.getCity()));
        request.setNeighborhood(inputSanitizer.sanitizeString(request.getNeighborhood()));
        request.setPropertyType(inputSanitizer.sanitizePropertyType(request.getPropertyType()));
        request.setMinPrice(inputSanitizer.sanitizeInteger(request.getMinPrice(), 0, 10000000));
        request.setMaxPrice(inputSanitizer.sanitizeInteger(request.getMaxPrice(), 0, 10000000));
        request.setBedrooms(inputSanitizer.sanitizeInteger(request.getBedrooms(), 0, 20));
        request.setBathrooms(inputSanitizer.sanitizeInteger(request.getBathrooms(), 0, 20));
        request.setAmenities(inputSanitizer.sanitizeList(request.getAmenities()));
        request.setMaxDistance(inputSanitizer.sanitizeDistance(request.getMaxDistance()));
        request.setUserLat(inputSanitizer.sanitizeLatitude(request.getUserLat()));
        request.setUserLon(inputSanitizer.sanitizeLongitude(request.getUserLon()));
        request.setAvailableFrom(inputSanitizer.sanitizeDate(request.getAvailableFrom()));
    }
}
