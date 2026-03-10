package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedSearchRequest {
    
    @NotBlank(message = "Search name is required")
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
}
