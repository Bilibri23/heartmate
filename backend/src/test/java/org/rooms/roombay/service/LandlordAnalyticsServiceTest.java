package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.response.LandlordAnalyticsResponse;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.Visit;
import org.rooms.roombay.repository.ListingFavoriteRepository;
import org.rooms.roombay.repository.ListingPhotoRepository;
import org.rooms.roombay.repository.ListingViewRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.VisitRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandlordAnalyticsServiceTest {

    @Mock
    private PropertyListingRepository listingRepository;
    @Mock
    private ListingViewRepository listingViewRepository;
    @Mock
    private ListingFavoriteRepository listingFavoriteRepository;
    @Mock
    private RoomApplicationRepository applicationRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private ListingPhotoRepository photoRepository;

    @InjectMocks
    private LandlordAnalyticsService landlordAnalyticsService;

    @Test
    void parseRangeDaysHandlesKnownValues() {
        assertThat(LandlordAnalyticsService.parseRangeDays("7d")).isEqualTo(7);
        assertThat(LandlordAnalyticsService.parseRangeDays("30d")).isEqualTo(30);
        assertThat(LandlordAnalyticsService.parseRangeDays("90d")).isEqualTo(90);
        assertThat(LandlordAnalyticsService.parseRangeDays(null)).isEqualTo(30);
    }

    @Test
    void percentChangeHandlesZeroPrevious() {
        assertThat(LandlordAnalyticsService.percentChange(0, 5)).isEqualTo(100);
        assertThat(LandlordAnalyticsService.percentChange(0, 0)).isZero();
        assertThat(LandlordAnalyticsService.percentChange(10, 15)).isEqualTo(50);
    }

    @Test
    void getAnalyticsAggregatesPeriodMetrics() {
        UUID landlordId = UUID.randomUUID();
        PropertyListing activeListing = PropertyListing.builder()
                .id(UUID.randomUUID())
                .landlord(org.rooms.roombay.entity.User.builder().id(landlordId).build())
                .title("Studio in Buea")
                .status(PropertyListing.Status.ACTIVE)
                .viewsCount(42)
                .favoritesCount(3)
                .rentAmount(50000)
                .city("Buea")
                .neighborhood("Molyko")
                .description("A".repeat(90))
                .createdAt(LocalDateTime.now().minusDays(20))
                .updatedAt(LocalDateTime.now())
                .build();

        when(listingRepository.findByLandlordId(landlordId)).thenReturn(List.of(activeListing));
        when(listingViewRepository.countByLandlordIdAndCreatedAtBetween(eq(landlordId), any(), any()))
                .thenReturn(12L, 8L);
        when(listingFavoriteRepository.countByLandlordIdAndCreatedAtBetween(eq(landlordId), any(), any()))
                .thenReturn(4L, 2L);
        when(applicationRepository.countByLandlordIdAndCreatedAtBetween(eq(landlordId), any(), any()))
                .thenReturn(3L, 1L);
        when(applicationRepository.countByLandlordIdAndStatus(landlordId, RoomApplication.Status.PENDING)).thenReturn(2L);
        when(applicationRepository.countByLandlordIdAndStatus(landlordId, RoomApplication.Status.ACCEPTED)).thenReturn(1L);
        when(applicationRepository.countByLandlordIdAndStatus(landlordId, RoomApplication.Status.REJECTED)).thenReturn(0L);
        when(applicationRepository.countByLandlordIdAndStatusAndCreatedAtBetween(
                eq(landlordId), eq(RoomApplication.Status.ACCEPTED), any(), any())).thenReturn(1L);
        when(visitRepository.countByLandlordIdAndStatus(landlordId, Visit.Status.REQUESTED)).thenReturn(1L);
        when(visitRepository.countByLandlordIdAndStatus(landlordId, Visit.Status.ACCEPTED)).thenReturn(2L);
        when(visitRepository.countByLandlordIdAndStatus(landlordId, Visit.Status.COMPLETED)).thenReturn(1L);
        when(visitRepository.countByLandlordIdAndCreatedAtBetween(eq(landlordId), any(), any())).thenReturn(2L);
        when(listingViewRepository.countViewsByListingForLandlordSince(eq(landlordId), any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{activeListing.getId(), 12L}));
        when(applicationRepository.countByListingId(activeListing.getId())).thenReturn(3L);
        when(photoRepository.findByListingId(activeListing.getId())).thenReturn(List.of());

        LandlordAnalyticsResponse response = landlordAnalyticsService.getAnalytics(landlordId, "30d");

        assertThat(response.getRange()).isEqualTo("30d");
        assertThat(response.getTotalViews()).isEqualTo(12);
        assertThat(response.getViewsChange()).isEqualTo(50);
        assertThat(response.getActiveListings()).isEqualTo(1);
        assertThat(response.getPendingApplications()).isEqualTo(2);
        assertThat(response.getVisitsCompleted()).isEqualTo(1);
        assertThat(response.getFunnel().getViews()).isEqualTo(12);
        assertThat(response.getTopListings()).hasSize(1);
        assertThat(response.getTopListings().get(0).getTitle()).isEqualTo("Studio in Buea");
    }
}
