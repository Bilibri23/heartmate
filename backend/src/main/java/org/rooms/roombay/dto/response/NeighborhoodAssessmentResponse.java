package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregate crowdsourced quality score for a city+neighborhood. All rating fields are null when
 * {@code reviewCount} is 0 — a separate signal from {@code AreaStatsResponse} (rent), computed
 * independently so the existing rent heatmap query stays untouched.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NeighborhoodAssessmentResponse {

    private String city;
    private String neighborhood;
    private Double avgSafety;
    private Double avgAmenities;
    private Double avgTransport;
    private Double avgNoise;
    private Double overallScore;
    private Long reviewCount;
}
