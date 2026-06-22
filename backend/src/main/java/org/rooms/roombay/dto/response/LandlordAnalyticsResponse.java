package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandlordAnalyticsResponse {

    private String range;
    private int periodDays;

    private long totalViews;
    private int viewsChange;
    private long totalFavorites;
    private int favoritesChange;
    private long totalApplications;
    private int applicationsChange;

    private int activeListings;
    private int rentedListings;
    private int occupancyRate;
    private Integer avgTimeToRentDays;
    private Integer avgListingQuality;

    private long pendingApplications;
    private long acceptedApplications;
    private long rejectedApplications;

    private long visitsRequested;
    private long visitsAccepted;
    private long visitsCompleted;

    private FunnelMetrics funnel;
    private List<TopListingMetrics> topListings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunnelMetrics {
        private long views;
        private long favorites;
        private long applications;
        private long visits;
        private long acceptedApplications;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopListingMetrics {
        private UUID id;
        private String title;
        private long views;
        private long favorites;
        private long applications;
        private Integer qualityScore;
    }
}
