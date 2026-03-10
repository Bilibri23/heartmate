package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedSearchResponse {
    
    private UUID id;
    private UUID userId;
    private String name;
    private String query;
    private String city;
    private String neighborhood;
    private String propertyType;
    private Integer minPrice;
    private Integer maxPrice;
    private Integer bedrooms;
    private Integer bathrooms;
    private List<String> amenities;
    private Double maxDistance;
    private Double userLat;
    private Double userLon;
    private String availableFrom;
    private Boolean notifyNewListings;
    private Boolean notifyPriceDrops;
    private LocalDateTime createdAt;
    private LocalDateTime lastCheckedAt;
    private Integer newListingsCount; // Number of new listings since last check
}
