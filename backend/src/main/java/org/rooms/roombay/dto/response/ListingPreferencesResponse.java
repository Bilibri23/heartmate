package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingPreferencesResponse {
    private UUID id;
    private UUID userId;
    private Integer minBudget;
    private Integer maxBudget;
    private List<String> preferredLocations;
    private BigDecimal maxDistanceFromCampus;
    private List<String> propertyTypes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
