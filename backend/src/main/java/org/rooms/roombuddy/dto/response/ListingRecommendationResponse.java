package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    // Recommendation metadata
    private Integer matchScore; // 0-100
    private Integer preferenceScore;
    private Integer behaviorScore;
    private List<String> reasons; // Why recommended
    
    // Engagement signals
    private Boolean isViewed;
    private Boolean isFavorited;
    private Integer viewsCount;
    
    // Verification
    private Boolean verified;
    private Boolean featured;
}
