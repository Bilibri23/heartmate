package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingPreferencesRequest {
    
    // Budget Preferences
    @Min(value = 0, message = "Min budget must be positive")
    private Integer minBudget;
    
    @Min(value = 0, message = "Max budget must be positive")
    private Integer maxBudget;
    
    // Location Preferences
    private List<String> preferredLocations;
    
    @DecimalMin(value = "0.0", message = "Max distance must be positive")
    private BigDecimal maxDistanceFromCampus;

    // Property type preferences (APARTMENT, HOUSE, STUDIO, SHARED_ROOM, PRIVATE_ROOM)
    private List<String> propertyTypes;
}
