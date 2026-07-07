package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.request.NeighborhoodReviewRequest;
import org.rooms.roombay.dto.response.NeighborhoodAssessmentResponse;
import org.rooms.roombay.dto.response.NeighborhoodReviewResponse;
import org.rooms.roombay.entity.NeighborhoodReview;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.repository.NeighborhoodReviewRepository;
import org.rooms.roombay.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NeighborhoodReviewServiceTest {

    @Mock private NeighborhoodReviewRepository reviewRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private NeighborhoodReviewService service;

    private static User user(UUID id) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Amina");
        u.setLastName("Tenant");
        return u;
    }

    private static NeighborhoodReviewRequest request() {
        return NeighborhoodReviewRequest.builder()
                .city("Douala")
                .neighborhood("Bonapriso")
                .safetyRating(4)
                .amenitiesRating(5)
                .transportRating(3)
                .noiseRating(2)
                .comment("Quiet at night, close to shops")
                .build();
    }

    @Test
    void submitCreatesNewReviewWhenNoneExists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(reviewRepository.findByReviewerIdAndCityIgnoreCaseAndNeighborhoodIgnoreCase(userId, "Douala", "Bonapriso"))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            NeighborhoodReview r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        NeighborhoodReviewResponse out = service.submitOrUpdateReview(userId, request());

        assertThat(out.getSafetyRating()).isEqualTo(4);
        assertThat(out.getReviewerFirstName()).isEqualTo("Amina");
        verify(reviewRepository, times(1)).save(any());
    }

    @Test
    void resubmitUpdatesExistingReviewInsteadOfDuplicating() {
        UUID userId = UUID.randomUUID();
        NeighborhoodReview existing = NeighborhoodReview.builder()
                .id(UUID.randomUUID())
                .reviewer(user(userId))
                .city("Douala")
                .neighborhood("Bonapriso")
                .safetyRating(2)
                .amenitiesRating(2)
                .transportRating(2)
                .noiseRating(2)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(reviewRepository.findByReviewerIdAndCityIgnoreCaseAndNeighborhoodIgnoreCase(userId, "Douala", "Bonapriso"))
                .thenReturn(Optional.of(existing));
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NeighborhoodReviewResponse out = service.submitOrUpdateReview(userId, request());

        assertThat(out.getId()).isEqualTo(existing.getId().toString());
        assertThat(out.getSafetyRating()).isEqualTo(4);
    }

    @Test
    void assessmentMapsAggregateRowToResponse() {
        when(reviewRepository.aggregateRatingsForArea("Douala", "Bonapriso"))
                .thenReturn(new Object[]{4.0, 5.0, 3.0, 2.0, 6L});

        NeighborhoodAssessmentResponse out = service.getAssessment("Douala", "Bonapriso");

        assertThat(out.getReviewCount()).isEqualTo(6L);
        assertThat(out.getAvgSafety()).isEqualTo(4.0);
        assertThat(out.getOverallScore()).isEqualTo((4.0 + 5.0 + 3.0 + 2.0) / 4.0);
    }

    @Test
    void assessmentWithNoReviewsReturnsZeroCountAndNullScores() {
        when(reviewRepository.aggregateRatingsForArea("Douala", "Akwa"))
                .thenReturn(new Object[]{null, null, null, null, 0L});

        NeighborhoodAssessmentResponse out = service.getAssessment("Douala", "Akwa");

        assertThat(out.getReviewCount()).isEqualTo(0L);
        assertThat(out.getAvgSafety()).isNull();
        assertThat(out.getOverallScore()).isNull();
    }
}
