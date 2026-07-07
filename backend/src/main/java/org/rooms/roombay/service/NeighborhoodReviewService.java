package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.NeighborhoodReviewRequest;
import org.rooms.roombay.dto.response.NeighborhoodAssessmentResponse;
import org.rooms.roombay.dto.response.NeighborhoodReviewResponse;
import org.rooms.roombay.entity.NeighborhoodReview;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.NeighborhoodReviewRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** RoomBay 2.0 — Phase 5: crowdsourced neighborhood quality ratings. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NeighborhoodReviewService {

    private final NeighborhoodReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public NeighborhoodReviewResponse submitOrUpdateReview(UUID userId, NeighborhoodReviewRequest request) {
        User reviewer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        NeighborhoodReview review = reviewRepository
                .findByReviewerIdAndCityIgnoreCaseAndNeighborhoodIgnoreCase(
                        userId, request.getCity(), request.getNeighborhood())
                .orElseGet(() -> NeighborhoodReview.builder()
                        .reviewer(reviewer)
                        .city(request.getCity())
                        .neighborhood(request.getNeighborhood())
                        .build());

        review.setSafetyRating(request.getSafetyRating());
        review.setAmenitiesRating(request.getAmenitiesRating());
        review.setTransportRating(request.getTransportRating());
        review.setNoiseRating(request.getNoiseRating());
        review.setComment(request.getComment());

        NeighborhoodReview saved = reviewRepository.save(review);
        log.info("Neighborhood review upserted: user={} city={} neighborhood={}", userId, saved.getCity(), saved.getNeighborhood());
        return NeighborhoodReviewResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<NeighborhoodReviewResponse> getReviews(String city, String neighborhood, Pageable pageable) {
        return reviewRepository
                .findByCityIgnoreCaseAndNeighborhoodIgnoreCaseOrderByCreatedAtDesc(city, neighborhood, pageable)
                .map(NeighborhoodReviewResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public NeighborhoodAssessmentResponse getAssessment(String city, String neighborhood) {
        Object[] row = reviewRepository.aggregateRatingsForArea(city, neighborhood);
        Long count = row == null || row[4] == null ? 0L : (Long) row[4];

        if (count == 0) {
            return NeighborhoodAssessmentResponse.builder()
                    .city(city)
                    .neighborhood(neighborhood)
                    .reviewCount(0L)
                    .build();
        }

        Double safety = (Double) row[0];
        Double amenities = (Double) row[1];
        Double transport = (Double) row[2];
        Double noise = (Double) row[3];
        double overall = (safety + amenities + transport + noise) / 4.0;

        return NeighborhoodAssessmentResponse.builder()
                .city(city)
                .neighborhood(neighborhood)
                .avgSafety(safety)
                .avgAmenities(amenities)
                .avgTransport(transport)
                .avgNoise(noise)
                .overallScore(overall)
                .reviewCount(count)
                .build();
    }
}
