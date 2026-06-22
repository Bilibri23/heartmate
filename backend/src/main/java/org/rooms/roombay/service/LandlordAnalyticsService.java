package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import org.rooms.roombay.dto.response.LandlordAnalyticsResponse;
import org.rooms.roombay.dto.response.PhotoDTO;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.Visit;
import org.rooms.roombay.repository.ListingFavoriteRepository;
import org.rooms.roombay.repository.ListingPhotoRepository;
import org.rooms.roombay.repository.ListingViewRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.VisitRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LandlordAnalyticsService {

    private final PropertyListingRepository listingRepository;
    private final ListingViewRepository listingViewRepository;
    private final ListingFavoriteRepository listingFavoriteRepository;
    private final RoomApplicationRepository applicationRepository;
    private final VisitRepository visitRepository;
    private final ListingPhotoRepository photoRepository;

    @Transactional(readOnly = true)
    public LandlordAnalyticsResponse getAnalytics(UUID landlordId, String range) {
        int periodDays = parseRangeDays(range);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.minusDays(periodDays);
        LocalDateTime previousStart = periodStart.minusDays(periodDays);

        List<PropertyListing> listings = listingRepository.findByLandlordId(landlordId);
        int activeListings = (int) listings.stream()
                .filter(l -> l.getStatus() == PropertyListing.Status.ACTIVE)
                .count();
        int rentedListings = (int) listings.stream()
                .filter(l -> l.getStatus() == PropertyListing.Status.RENTED)
                .count();
        int occupancyRate = listings.isEmpty()
                ? 0
                : Math.round(rentedListings * 100f / listings.size());

        long periodViews = listingViewRepository.countByLandlordIdAndCreatedAtBetween(
                landlordId, periodStart, now);
        long previousViews = listingViewRepository.countByLandlordIdAndCreatedAtBetween(
                landlordId, previousStart, periodStart);

        long periodFavorites = listingFavoriteRepository.countByLandlordIdAndCreatedAtBetween(
                landlordId, periodStart, now);
        long previousFavorites = listingFavoriteRepository.countByLandlordIdAndCreatedAtBetween(
                landlordId, previousStart, periodStart);

        long periodApplications = applicationRepository.countByLandlordIdAndCreatedAtBetween(
                landlordId, periodStart, now);
        long previousApplications = applicationRepository.countByLandlordIdAndCreatedAtBetween(
                landlordId, previousStart, periodStart);

        long pendingApplications = applicationRepository.countByLandlordIdAndStatus(
                landlordId, RoomApplication.Status.PENDING);
        long acceptedApplications = applicationRepository.countByLandlordIdAndStatus(
                landlordId, RoomApplication.Status.ACCEPTED);
        long rejectedApplications = applicationRepository.countByLandlordIdAndStatus(
                landlordId, RoomApplication.Status.REJECTED);

        long visitsRequested = visitRepository.countByLandlordIdAndStatus(landlordId, Visit.Status.REQUESTED);
        long visitsAccepted = visitRepository.countByLandlordIdAndStatus(landlordId, Visit.Status.ACCEPTED);
        long visitsCompleted = visitRepository.countByLandlordIdAndStatus(landlordId, Visit.Status.COMPLETED);

        long periodVisits = visitRepository.countByLandlordIdAndCreatedAtBetween(landlordId, periodStart, now);
        long periodAcceptedApplications = applicationRepository.countByLandlordIdAndStatusAndCreatedAtBetween(
                landlordId, RoomApplication.Status.ACCEPTED, periodStart, now);

        Map<UUID, PropertyListing> listingById = listings.stream()
                .collect(Collectors.toMap(PropertyListing::getId, l -> l, (a, b) -> a));

        List<LandlordAnalyticsResponse.TopListingMetrics> topListings = buildTopListings(
                landlordId, periodStart, listingById);

        return LandlordAnalyticsResponse.builder()
                .range(normalizeRange(range, periodDays))
                .periodDays(periodDays)
                .totalViews(periodViews)
                .viewsChange(percentChange(previousViews, periodViews))
                .totalFavorites(periodFavorites)
                .favoritesChange(percentChange(previousFavorites, periodFavorites))
                .totalApplications(periodApplications)
                .applicationsChange(percentChange(previousApplications, periodApplications))
                .activeListings(activeListings)
                .rentedListings(rentedListings)
                .occupancyRate(occupancyRate)
                .avgTimeToRentDays(computeAvgTimeToRentDays(listings))
                .avgListingQuality(computeAvgListingQuality(listings))
                .pendingApplications(pendingApplications)
                .acceptedApplications(acceptedApplications)
                .rejectedApplications(rejectedApplications)
                .visitsRequested(visitsRequested)
                .visitsAccepted(visitsAccepted)
                .visitsCompleted(visitsCompleted)
                .funnel(LandlordAnalyticsResponse.FunnelMetrics.builder()
                        .views(periodViews)
                        .favorites(periodFavorites)
                        .applications(periodApplications)
                        .visits(periodVisits)
                        .acceptedApplications(periodAcceptedApplications)
                        .build())
                .topListings(topListings)
                .build();
    }

    private List<LandlordAnalyticsResponse.TopListingMetrics> buildTopListings(
            UUID landlordId,
            LocalDateTime periodStart,
            Map<UUID, PropertyListing> listingById) {
        List<Object[]> ranked = listingViewRepository.countViewsByListingForLandlordSince(
                landlordId, periodStart, PageRequest.of(0, 5));
        List<LandlordAnalyticsResponse.TopListingMetrics> topListings = new ArrayList<>();
        for (Object[] row : ranked) {
            UUID listingId = (UUID) row[0];
            long periodViewCount = (Long) row[1];
            PropertyListing listing = listingById.get(listingId);
            if (listing == null) {
                continue;
            }
            topListings.add(LandlordAnalyticsResponse.TopListingMetrics.builder()
                    .id(listingId)
                    .title(listing.getTitle())
                    .views(periodViewCount)
                    .favorites(listing.getFavoritesCount() != null ? listing.getFavoritesCount() : 0)
                    .applications(applicationRepository.countByListingId(listingId))
                    .qualityScore(computeListingQuality(listing))
                    .build());
        }
        return topListings;
    }

    private Integer computeListingQuality(PropertyListing listing) {
        List<PhotoDTO> photos = photoRepository.findByListingId(listing.getId()).stream()
                .map(photo -> PhotoDTO.builder()
                        .id(photo.getId())
                        .photoUrl(photo.getPhotoUrl())
                        .isPrimary(photo.getIsPrimary())
                        .displayOrder(photo.getDisplayOrder())
                        .build())
                .toList();
        Map<String, Boolean> signals = ListingService.qualitySignals(listing, photos);
        return ListingService.qualityScore(signals);
    }

    private Integer computeAvgListingQuality(List<PropertyListing> listings) {
        if (listings.isEmpty()) {
            return null;
        }
        int total = 0;
        int count = 0;
        for (PropertyListing listing : listings) {
            if (listing.getStatus() == PropertyListing.Status.DELETED) {
                continue;
            }
            total += computeListingQuality(listing);
            count++;
        }
        return count == 0 ? null : Math.round((float) total / count);
    }

    private Integer computeAvgTimeToRentDays(List<PropertyListing> listings) {
        List<Long> daysToRent = listings.stream()
                .filter(l -> l.getStatus() == PropertyListing.Status.RENTED)
                .filter(l -> l.getCreatedAt() != null && l.getUpdatedAt() != null)
                .map(l -> ChronoUnit.DAYS.between(l.getCreatedAt(), l.getUpdatedAt()))
                .filter(days -> days >= 0)
                .toList();
        if (daysToRent.isEmpty()) {
            return null;
        }
        long average = Math.round(daysToRent.stream().mapToLong(Long::longValue).average().orElse(0));
        return (int) average;
    }

    static int parseRangeDays(String range) {
        if (range == null || range.isBlank()) {
            return 30;
        }
        return switch (range.trim().toLowerCase()) {
            case "7d" -> 7;
            case "90d" -> 90;
            default -> 30;
        };
    }

    private static String normalizeRange(String range, int periodDays) {
        if (range != null && !range.isBlank()) {
            return range.trim().toLowerCase();
        }
        return periodDays + "d";
    }

    static int percentChange(long previous, long current) {
        if (previous == 0) {
            return current > 0 ? 100 : 0;
        }
        return (int) Math.round(((current - previous) * 100.0) / previous);
    }
}
