package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.ListingRequest;
import org.rooms.roombuddy.dto.response.ApiResponse;
import org.rooms.roombuddy.dto.response.ListingResponse;
import org.rooms.roombuddy.security.RequiresVerification;
import org.rooms.roombuddy.service.FileUploadService;
import org.rooms.roombuddy.service.ListingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Property Listings", description = "APIs for property listings")
public class ListingController {
    
    private final ListingService listingService;
    private final FileUploadService fileUploadService;
    
    @PostMapping
    @Operation(summary = "Create listing", description = "Create a new property listing (Landlord only)")
    @RequiresVerification(role = "LANDLORD", verificationType = "IDENTITY")
    public ResponseEntity<ListingResponse> createListing(
            @Valid @RequestBody ListingRequest request,
            @RequestParam UUID landlordId) {
        log.info("Creating listing for landlord: {}", landlordId);
        ListingResponse response = listingService.createListing(landlordId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{listingId}")
    @Operation(summary = "Get listing", description = "Get listing details by ID")
    public ResponseEntity<ListingResponse> getListing(
            @PathVariable UUID listingId,
            @RequestParam(required = false) UUID userId) {
        log.info("Fetching listing: {}", listingId);
        ListingResponse response = listingService.getListing(listingId, userId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{listingId}")
    @Operation(summary = "Update listing", description = "Update a property listing (Landlord only)")
    @RequiresVerification(role = "LANDLORD", verificationType = "IDENTITY")
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable UUID listingId,
            @RequestParam UUID landlordId,
            @Valid @RequestBody ListingRequest request) {
        log.info("Updating listing: {} for landlord: {}", listingId, landlordId);
        ListingResponse response = listingService.updateListing(listingId, landlordId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{listingId}")
    @Operation(summary = "Delete listing", description = "Delete a property listing (Landlord only)")
    public ResponseEntity<Void> deleteListing(
            @PathVariable UUID listingId,
            @RequestParam UUID landlordId) {
        log.info("Deleting listing: {} by landlord: {}", listingId, landlordId);
        listingService.deleteListing(listingId, landlordId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    @Operation(summary = "Search listings", description = "Search and filter property listings with pagination")
    public ResponseEntity<Page<ListingResponse>> searchListings(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("Searching listings with filters - page: {}, size: {}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ListingResponse> listings = (Page<ListingResponse>) listingService.searchListings(
                city, neighborhood, propertyType, minPrice, maxPrice, amenities, userId, pageable);
        return ResponseEntity.ok(listings);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active listings", description = "Get all active and verified listings")
    public ResponseEntity<List<ListingResponse>> getActiveListings(
            @RequestParam(required = false) UUID userId) {
        log.info("Getting active listings");
        List<ListingResponse> listings = listingService.getActiveListings(userId);
        return ResponseEntity.ok(listings);
    }
    
    @GetMapping("/featured")
    @Operation(summary = "Get featured listings", description = "Get all featured listings")
    public ResponseEntity<List<ListingResponse>> getFeaturedListings(
            @RequestParam(required = false) UUID userId) {
        log.info("Getting featured listings");
        List<ListingResponse> listings = listingService.getFeaturedListings(userId);
        return ResponseEntity.ok(listings);
    }
    
    @GetMapping("/landlord/{landlordId}")
    @Operation(summary = "Get landlord listings", description = "Get all listings for a landlord")
    public ResponseEntity<List<ListingResponse>> getLandlordListings(@PathVariable UUID landlordId) {
        log.info("Getting listings for landlord: {}", landlordId);
        List<ListingResponse> listings = listingService.getLandlordListings(landlordId);
        return ResponseEntity.ok(listings);
    }
    
    @PostMapping("/{listingId}/photos")
    @Operation(summary = "Add photo to listing", description = "Add a photo to a listing (Landlord only)")
    @RequiresVerification(role = "LANDLORD", verificationType = "IDENTITY")
    public ResponseEntity<ListingResponse> addPhoto(
            @PathVariable UUID listingId,
            @RequestParam UUID landlordId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "false") Boolean isPrimary) {
        log.info("Adding photo to listing: {}", listingId);
        
        // Upload photo to Cloudinary
        String photoUrl = fileUploadService.uploadListingPhoto(file);
        
        ListingResponse response = listingService.addPhoto(listingId, landlordId, photoUrl, isPrimary);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/photos/{photoId}")
    @Operation(summary = "Remove photo from listing", description = "Remove a photo from a listing (Landlord only)")
    public ResponseEntity<Void> removePhoto(
            @PathVariable UUID photoId,
            @RequestParam UUID landlordId) {
        log.info("Removing photo: {}", photoId);
        listingService.removePhoto(photoId, landlordId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{listingId}/favorite")
    @Operation(summary = "Toggle favorite", description = "Add or remove listing from favorites")
    public ResponseEntity<ApiResponse<ListingResponse>> toggleFavorite(
            @PathVariable UUID listingId,
            @RequestParam UUID userId) {
        log.info("Toggling favorite for listing: {} by user: {}", listingId, userId);
        ListingResponse response = listingService.toggleFavorite(listingId, userId);
        return ResponseEntity.ok(ApiResponse.<ListingResponse>builder()
                .success(true)
                .message("Favorite toggled successfully")
                .data(response)
                .build());
    }
    
    @GetMapping("/favorites/{userId}")
    @Operation(summary = "Get favorites", description = "Get all favorite listings for a user")
    public ResponseEntity<List<ListingResponse>> getFavorites(@PathVariable UUID userId) {
        log.info("Getting favorites for user: {}", userId);
        List<ListingResponse> favorites = listingService.getFavorites(userId);
        return ResponseEntity.ok(favorites);
    }
    
    @PostMapping("/{listingId}/mark-rented")
    @Operation(summary = "Mark as rented", description = "Mark a listing as rented (Landlord only)")
    public ResponseEntity<ListingResponse> markAsRented(
            @PathVariable UUID listingId,
            @RequestParam UUID landlordId) {
        log.info("Marking listing {} as rented by landlord: {}", listingId, landlordId);
        ListingResponse response = listingService.markAsRented(listingId, landlordId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{listingId}/mark-available")
    @Operation(summary = "Mark as available", description = "Mark a listing as available again (Landlord only)")
    public ResponseEntity<ListingResponse> markAsAvailable(
            @PathVariable UUID listingId,
            @RequestParam UUID landlordId) {
        log.info("Marking listing {} as available by landlord: {}", listingId, landlordId);
        ListingResponse response = listingService.markAsAvailable(listingId, landlordId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{listingId}/view")
    @Operation(summary = "Track view", description = "Track a view for a listing")
    public ResponseEntity<Void> trackView(
            @PathVariable UUID listingId,
            @RequestParam(required = false) UUID userId) {
        log.info("Tracking view for listing: {} by user: {}", listingId, userId);
        listingService.trackView(listingId, userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/landlord/{landlordId}/statistics")
    @Operation(summary = "Get landlord statistics", description = "Get statistics for a landlord's listings")
    public ResponseEntity<Map<String, Object>> getLandlordStatistics(@PathVariable UUID landlordId) {
        log.info("Getting statistics for landlord: {}", landlordId);
        Map<String, Object> statistics = listingService.getLandlordStatistics(landlordId);
        return ResponseEntity.ok(statistics);
    }
}

