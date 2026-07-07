package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A crowdsourced quality rating for a city+neighborhood pair (safety, amenities, transport, noise).
 * Free-text city/neighborhood, matching the convention on {@link PropertyListing} — no master
 * "Neighborhood" reference table. One row per reviewer per area (enforced by a DB unique index);
 * resubmission updates the existing row.
 */
@Entity
@Table(name = "neighborhood_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NeighborhoodReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String neighborhood;

    @Column(name = "safety_rating", nullable = false)
    private Integer safetyRating;

    @Column(name = "amenities_rating", nullable = false)
    private Integer amenitiesRating;

    @Column(name = "transport_rating", nullable = false)
    private Integer transportRating;

    @Column(name = "noise_rating", nullable = false)
    private Integer noiseRating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
