package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Submit or update a crowdsourced quality rating for a city+neighborhood. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NeighborhoodReviewRequest {

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Neighborhood is required")
    private String neighborhood;

    @NotNull @Min(1) @Max(5)
    private Integer safetyRating;

    @NotNull @Min(1) @Max(5)
    private Integer amenitiesRating;

    @NotNull @Min(1) @Max(5)
    private Integer transportRating;

    @NotNull @Min(1) @Max(5)
    private Integer noiseRating;

    @Size(max = 1000, message = "Comment must be less than 1000 characters")
    private String comment;
}
