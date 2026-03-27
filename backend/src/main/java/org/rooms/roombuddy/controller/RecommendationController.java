package org.rooms.roombay.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.ListingRecommendationResponse;
import org.rooms.roombay.dto.response.MatchResponse;
import org.rooms.roombay.entity.ListingPhoto;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.repository.ListingFavoriteRepository;
import org.rooms.roombay.repository.ListingPhotoRepository;
import org.rooms.roombay.repository.ListingViewRepository;
import org.rooms.roombay.repository.ReviewRepository;
import org.rooms.roombay.service.MatchingService;
import org.rooms.roombay.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Endpoints for personalized recommendations
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {
    
    private final RecommendationService recommendationService;
    private final MatchingService matchingService;
    private final ListingPhotoRepository photoRepository;
    private final ListingViewRepository viewRepository;
    private final ListingFavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    
    /**
     * GET /api/recommendations/listings
     * Get personalized listing recommendations for current user
     */
    @GetMapping("/listings")
    public ResponseEntity<List<ListingRecommendationResponse>> getListingRecommendations(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (userDetails == null) {
            log.warn("No authenticated user for recommendations request");
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("Getting listing recommendations for user: {}", userId);
        
        try {
            List<RecommendationService.ScoredListing> recommendations = 
                recommendationService.getRecommendedListings(userId);
            
            List<ListingRecommendationResponse> response = recommendations.stream()
                .map(scored -> mapToResponse(scored, userId))
                .collect(Collectors.toList());
            
            log.info("Returning {} recommendations for user {}", response.size(), userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating recommendations for user {}", userId, e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
    
    /**
     * GET /api/recommendations/roommates
     * Get roommate match recommendations (uses existing matching algorithm)
     */
    @GetMapping("/roommates")
    public ResponseEntity<List<MatchResponse>> getRoommateRecommendations(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("Getting roommate recommendations for user: {}", userId);
        
        // Use existing matching service
        List<MatchResponse> matches = matchingService.findMatches(userId);
        
        return ResponseEntity.ok(matches);
    }
    
    /**
     * POST /api/recommendations/track-view
     * Track when user views a listing (for behavioral recommendations)
     */
    @PostMapping("/track-view")
    public ResponseEntity<Void> trackListingView(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam UUID listingId,
            @RequestParam(required = false, defaultValue = "unknown") String source) {
        
        if (userDetails == null) {
            return ResponseEntity.ok().build();
        }
        
        UUID userId = UUID.fromString(userDetails.getUsername());
        recommendationService.trackListingView(userId, listingId, source);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Map ScoredListing to Response DTO
     */
    private ListingRecommendationResponse mapToResponse(
            RecommendationService.ScoredListing scored, UUID userId) {
        
        PropertyListing listing = scored.getListing();
        
        // Get primary photo
        String primaryPhotoUrl = photoRepository
            .findByListingIdAndIsPrimary(listing.getId(), true)
            .map(ListingPhoto::getPhotoUrl)
            .orElse(null);
        
        // Check if user has viewed/favorited
        boolean isViewed = viewRepository.hasUserViewedListing(userId, listing.getId());
        boolean isFavorited = favoriteRepository.existsByUserIdAndListingId(userId, listing.getId());
        
        // Get rating information
        Double averageRating = reviewRepository.getAverageRatingForListing(listing.getId());
        long reviewCount = reviewRepository.countReviewsForListing(listing.getId());
        
        return ListingRecommendationResponse.builder()
            .listingId(listing.getId())
            .title(listing.getTitle())
            .description(listing.getDescription())
            .rentAmount(listing.getRentAmount())
            .city(listing.getCity())
            .neighborhood(listing.getNeighborhood())
            .propertyType(listing.getPropertyType() != null ? listing.getPropertyType().name() : null)
            .primaryPhotoUrl(primaryPhotoUrl)
            .bedrooms(listing.getBedrooms())
            .bathrooms(listing.getBathrooms())
            .amenities(listing.getAmenities())
            // Availability / Status
            .status(listing.getStatus() != null ? listing.getStatus().name() : null)
            .availableFrom(listing.getAvailableFrom())
            .availableTo(listing.getAvailableTo())
            .isAvailable(listing.getStatus() != null && listing.getStatus() == PropertyListing.Status.ACTIVE)
            // Recommendation metadata
            .matchScore(scored.getTotalScore())
            .preferenceScore(scored.getPreferenceScore())
            .behaviorScore(scored.getBehaviorScore())
            .reasons(scored.getReasons())
            // Engagement signals
            .isViewed(isViewed)
            .isFavorited(isFavorited)
            .viewsCount(listing.getViewsCount())
            // Verification
            .verified(listing.getVerified())
            .featured(listing.getFeatured())
            // Ratings
            .averageRating(averageRating)
            .reviewCount((int) reviewCount)
            .build();
    }
}
