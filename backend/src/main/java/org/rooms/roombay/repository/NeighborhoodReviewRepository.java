package org.rooms.roombay.repository;

import org.rooms.roombay.entity.NeighborhoodReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NeighborhoodReviewRepository extends JpaRepository<NeighborhoodReview, UUID> {

    Optional<NeighborhoodReview> findByReviewerIdAndCityIgnoreCaseAndNeighborhoodIgnoreCase(
            UUID reviewerId, String city, String neighborhood);

    Page<NeighborhoodReview> findByCityIgnoreCaseAndNeighborhoodIgnoreCaseOrderByCreatedAtDesc(
            String city, String neighborhood, Pageable pageable);

    /**
     * Aggregate quality ratings for a city+neighborhood: mirrors
     * {@link PropertyListingRepository#aggregateRentByNeighborhood}, but for the crowdsourced
     * qualitative dimension rather than rent. Columns: [avgSafety, avgAmenities, avgTransport,
     * avgNoise, count].
     */
    @Query("""
            SELECT AVG(r.safetyRating), AVG(r.amenitiesRating), AVG(r.transportRating), AVG(r.noiseRating), COUNT(r)
            FROM NeighborhoodReview r
            WHERE LOWER(r.city) = LOWER(:city) AND LOWER(r.neighborhood) = LOWER(:neighborhood)
            """)
    Object[] aggregateRatingsForArea(@Param("city") String city, @Param("neighborhood") String neighborhood);
}
