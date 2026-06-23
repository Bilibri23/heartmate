package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.dto.request.RoomApplicationRequest;
import org.rooms.roombay.dto.request.VisitRequest;
import org.rooms.roombay.dto.response.ListingResponse;
import org.rooms.roombay.dto.response.RoomApplicationResponse;
import org.rooms.roombay.dto.response.VisitResponse;
import org.rooms.roombay.exception.BadRequestException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAgentToolServiceTest {

    private final ListingService listingService = mock(ListingService.class);
    private final ApplicationService applicationService = mock(ApplicationService.class);
    private final VisitService visitService = mock(VisitService.class);
    private final AnalyticsEventService analyticsEventService = mock(AnalyticsEventService.class);

    private final AiAgentToolService service = new AiAgentToolService(
            listingService, applicationService, visitService, analyticsEventService);

    @Test
    void rejectsUnknownTool() {
        var result = service.execute(UUID.randomUUID(), "STUDENT", "DELETE_LISTING", Map.of(), "r1");
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("not allowed");
    }

    @Test
    void rejectsNonStudentRole() {
        UUID listingId = UUID.randomUUID();
        var result = service.execute(
                UUID.randomUUID(), "LANDLORD", "SAVE_LISTING_FAVORITE", Map.of("listingId", listingId), "r1");
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Only tenants");
    }

    @Test
    void saveFavoriteDelegatesToListingService() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(listingService.toggleFavorite(listingId, userId)).thenReturn(
                ListingResponse.builder().id(listingId).title("Studio Damas").isFavorite(true).build());

        var result = service.execute(userId, "STUDENT", "SAVE_LISTING_FAVORITE", Map.of("listingId", listingId), "r1");

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("Saved");
        verify(listingService).toggleFavorite(listingId, userId);
    }

    @Test
    void applyToListingUsesDefaultsWhenMessageTooShort() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        when(applicationService.createApplication(eq(userId), any(RoomApplicationRequest.class))).thenReturn(
                RoomApplicationResponse.builder()
                        .id(applicationId)
                        .listingTitle("Cozy studio")
                        .build());

        var result = service.execute(
                userId, "STUDENT", "APPLY_TO_LISTING", Map.of("listingId", listingId, "message", "short"), "r1");

        assertThat(result.success()).isTrue();
        assertThat(result.entityId()).isEqualTo(applicationId.toString());
        verify(applicationService).createApplication(eq(userId), any(RoomApplicationRequest.class));
    }

    @Test
    void requestVisitDelegatesToVisitService() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        when(visitService.requestVisit(eq(userId), any(VisitRequest.class))).thenReturn(
                VisitResponse.builder().id(visitId).listingTitle("House Buea").build());

        var result = service.execute(userId, "STUDENT", "REQUEST_VISIT", Map.of("listingId", listingId), "r1");

        assertThat(result.success()).isTrue();
        assertThat(result.entityId()).isEqualTo(visitId.toString());
        verify(visitService).requestVisit(eq(userId), any(VisitRequest.class));
    }

    @Test
    void surfacesDomainValidationErrors() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(listingService.toggleFavorite(listingId, userId))
                .thenThrow(new BadRequestException("Listing not available"));

        var result = service.execute(userId, "STUDENT", "SAVE_LISTING_FAVORITE", Map.of("listingId", listingId), "r1");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("not available");
    }
}
