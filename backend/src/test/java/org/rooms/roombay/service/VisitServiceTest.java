package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.dto.request.VisitRequest;
import org.rooms.roombay.dto.request.VisitUpdateRequest;
import org.rooms.roombay.dto.response.VisitResponse;
import org.rooms.roombay.entity.Notification;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.entity.Visit;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.repository.ListingPhotoRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.UserRepository;
import org.rooms.roombay.repository.VisitRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisitServiceTest {

    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final PropertyListingRepository listingRepository = mock(PropertyListingRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoomApplicationRepository applicationRepository = mock(RoomApplicationRepository.class);
    private final ListingPhotoRepository listingPhotoRepository = mock(ListingPhotoRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AnalyticsEventService analyticsEventService = mock(AnalyticsEventService.class);

    private final VisitService service = new VisitService(
            visitRepository, listingRepository, userRepository, applicationRepository,
            listingPhotoRepository, notificationService, analyticsEventService);

    private User tenant(UUID id) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getRole()).thenReturn(User.UserRole.STUDENT);
        when(u.getFirstName()).thenReturn("Ada");
        when(u.getLastName()).thenReturn("N");
        return u;
    }

    private User landlord(UUID id) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getFirstName()).thenReturn("Bob");
        when(u.getLastName()).thenReturn("L");
        return u;
    }

    private PropertyListing activeListing(UUID id, User landlord) {
        PropertyListing l = mock(PropertyListing.class);
        when(l.getId()).thenReturn(id);
        when(l.getStatus()).thenReturn(PropertyListing.Status.ACTIVE);
        when(l.getLandlord()).thenReturn(landlord);
        when(l.getTitle()).thenReturn("Cozy studio");
        return l;
    }

    @Test
    void requestVisitCreatesRequestedAndNotifiesLandlord() {
        UUID tenantId = UUID.randomUUID();
        UUID landlordId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        // Build all mocks first; stubbing a mock inside another when(...) trips Mockito.
        User tenantUser = tenant(tenantId);
        User landlordUser = landlord(landlordId);
        PropertyListing listing = activeListing(listingId, landlordUser);

        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenantUser));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingPhotoRepository.findByListingIdAndIsPrimary(any(), eq(true))).thenReturn(Optional.empty());
        when(listingPhotoRepository.findFirstByListingIdOrderByDisplayOrderAsc(any())).thenReturn(Optional.empty());
        when(visitRepository.save(any(Visit.class))).thenAnswer(inv -> {
            Visit v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        VisitRequest req = VisitRequest.builder()
                .listingId(listingId)
                .requestedDatetime(LocalDateTime.now().plusDays(2))
                .message("Can I view it?")
                .build();

        VisitResponse res = service.requestVisit(tenantId, req);

        assertEquals(Visit.Status.REQUESTED, res.getStatus());
        assertEquals(landlordId, res.getLandlordId());
        verify(notificationService).createNotification(
                eq(landlordId), eq(Notification.NotificationType.VISIT_REQUESTED),
                any(), any(), any(), eq("VISIT"), any());
    }

    @Test
    void updateVisitRejectsNonOwningLandlord() {
        UUID visitId = UUID.randomUUID();
        UUID ownerLandlordId = UUID.randomUUID();
        Visit visit = Visit.builder()
                .id(visitId)
                .landlord(landlord(ownerLandlordId))
                .tenant(tenant(UUID.randomUUID()))
                .listing(activeListing(UUID.randomUUID(), landlord(ownerLandlordId)))
                .status(Visit.Status.REQUESTED)
                .requestedDatetime(LocalDateTime.now().plusDays(1))
                .build();
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        VisitUpdateRequest req = VisitUpdateRequest.builder().status(Visit.Status.ACCEPTED).build();

        assertThrows(BadRequestException.class,
                () -> service.updateVisit(visitId, UUID.randomUUID(), req));
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void acceptConfirmsRequestedTimeWhenNoNewTimeGiven() {
        UUID visitId = UUID.randomUUID();
        UUID landlordId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        LocalDateTime requested = LocalDateTime.now().plusDays(3);
        Visit visit = Visit.builder()
                .id(visitId)
                .landlord(landlord(landlordId))
                .tenant(tenant(tenantId))
                .listing(activeListing(UUID.randomUUID(), landlord(landlordId)))
                .status(Visit.Status.REQUESTED)
                .requestedDatetime(requested)
                .build();
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(listingPhotoRepository.findByListingIdAndIsPrimary(any(), eq(true))).thenReturn(Optional.empty());
        when(listingPhotoRepository.findFirstByListingIdOrderByDisplayOrderAsc(any())).thenReturn(Optional.empty());
        when(visitRepository.save(any(Visit.class))).thenAnswer(inv -> inv.getArgument(0));

        VisitUpdateRequest req = VisitUpdateRequest.builder().status(Visit.Status.ACCEPTED).build();
        VisitResponse res = service.updateVisit(visitId, landlordId, req);

        assertEquals(Visit.Status.ACCEPTED, res.getStatus());
        assertEquals(requested, res.getVisitDatetime());
        verify(notificationService).createNotification(
                eq(tenantId), eq(Notification.NotificationType.VISIT_ACCEPTED),
                any(), any(), any(), eq("VISIT"), any());
    }

    @Test
    void completeRequiresConfirmedVisit() {
        UUID visitId = UUID.randomUUID();
        UUID landlordId = UUID.randomUUID();
        Visit visit = Visit.builder()
                .id(visitId)
                .landlord(landlord(landlordId))
                .tenant(tenant(UUID.randomUUID()))
                .listing(activeListing(UUID.randomUUID(), landlord(landlordId)))
                .status(Visit.Status.REQUESTED) // not yet accepted
                .requestedDatetime(LocalDateTime.now().plusDays(1))
                .build();
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        VisitUpdateRequest req = VisitUpdateRequest.builder().status(Visit.Status.COMPLETED).build();
        assertThrows(BadRequestException.class, () -> service.updateVisit(visitId, landlordId, req));
    }
}
