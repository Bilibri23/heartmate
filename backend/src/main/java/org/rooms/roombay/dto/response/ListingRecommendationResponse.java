package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingRecommendationResponse {
    
    private UUID listingId;
    private String title;
    private String description;
    private Integer rentAmount;
    private String city;
    private String neighborhood;
    private String propertyType;
    private String primaryPhotoUrl;
    private Integer bedrooms;
    private Integer bathrooms;
    private List<String> amenities;
    
    // Availability / Status
    private String status; // e.g., ACTIVE, RENTED, INACTIVE
    private Boolean isAvailable; // derived convenience flag
    private LocalDate availableFrom;
    private LocalDate availableTo;
    
    // Recommendation metadata
    private Integer matchScore; // 0-100
    private Integer preferenceScore;
    private Integer behaviorScore;
    private List<String> reasons; // Why recommended (legacy / verbose)
    /** Stable keys for UI chips; prefer over {@code reasons} when present. */
    private List<String> reasonCodes;
    
    // Engagement signals
    private Boolean isViewed;
    private Boolean isFavorited;
    private Integer viewsCount;
    
    // Verification
    private Boolean verified;
    private Boolean featured;
    private String trustTier;
    
    // Ratings
    private Double averageRating;
    private Integer reviewCount;
}
