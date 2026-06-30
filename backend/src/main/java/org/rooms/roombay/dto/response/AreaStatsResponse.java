package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-neighborhood rent stats for the map heatmap overlay. The frontend joins {@code neighborhood}
 * to coordinates (lib/neighborhoods.ts) and shades the area by {@code avgRent}, sized by
 * {@code listingCount}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaStatsResponse {
    private String city;
    private String neighborhood;
    private long listingCount;
    private Double avgRent;
    private Integer minRent;
    private Integer maxRent;
}
