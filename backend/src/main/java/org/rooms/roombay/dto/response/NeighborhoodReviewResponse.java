package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.NeighborhoodReview;

import java.time.LocalDateTime;

/** Public view of a neighborhood review — reviewer identity limited to first name, no contact info. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NeighborhoodReviewResponse {

    private String id;
    private String reviewerFirstName;
    private String city;
    private String neighborhood;
    private Integer safetyRating;
    private Integer amenitiesRating;
    private Integer transportRating;
    private Integer noiseRating;
    private String comment;
    private LocalDateTime createdAt;

    public static NeighborhoodReviewResponse fromEntity(NeighborhoodReview r) {
        return NeighborhoodReviewResponse.builder()
                .id(r.getId() == null ? null : r.getId().toString())
                .reviewerFirstName(r.getReviewer() == null ? null : r.getReviewer().getFirstName())
                .city(r.getCity())
                .neighborhood(r.getNeighborhood())
                .safetyRating(r.getSafetyRating())
                .amenitiesRating(r.getAmenitiesRating())
                .transportRating(r.getTransportRating())
                .noiseRating(r.getNoiseRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
