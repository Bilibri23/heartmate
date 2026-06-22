package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.VisitRequest;
import org.rooms.roombay.dto.request.VisitUpdateRequest;
import org.rooms.roombay.dto.response.VisitResponse;
import org.rooms.roombay.entity.Notification;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.entity.Visit;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.ListingPhotoRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.UserRepository;
import org.rooms.roombay.repository.VisitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Visit scheduling service. Mirrors {@link ApplicationService}: ownership is re-checked
 * server-side on every mutation, and tenant/landlord are notified on each transition.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VisitService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm");

    private final VisitRepository visitRepository;
    private final PropertyListingRepository listingRepository;
    private final UserRepository userRepository;
    private final RoomApplicationRepository applicationRepository;
    private final ListingPhotoRepository listingPhotoRepository;
    private final NotificationService notificationService;
    private final AnalyticsEventService analyticsEventService;

    /** Tenant requests a visit. */
    @Transactional
    public VisitResponse requestVisit(UUID tenantId, VisitRequest request) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        if (tenant.getRole() != User.UserRole.STUDENT) {
            throw new BadRequestException("Only tenants can request visits");
        }

        PropertyListing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (listing.getStatus() != PropertyListing.Status.ACTIVE) {
            throw new BadRequestException("Cannot request a visit for an inactive listing");
        }

        User landlord = listing.getLandlord();
        if (landlord == null) {
            throw new BadRequestException("Listing has no landlord");
        }
        if (landlord.getId().equals(tenantId)) {
            throw new BadRequestException("You cannot request a visit to your own listing");
        }

        RoomApplication application = null;
        if (request.getApplicationId() != null) {
            application = applicationRepository.findById(request.getApplicationId()).orElse(null);
            // Only link an application the tenant owns and that belongs to this listing.
            if (application != null && (!application.getStudent().getId().equals(tenantId)
                    || !application.getListing().getId().equals(listing.getId()))) {
                application = null;
            }
        }

        Visit visit = Visit.builder()
                .listing(listing)
                .tenant(tenant)
                .landlord(landlord)
                .application(application)
                .requestedDatetime(request.getRequestedDatetime())
                .tenantMessage(request.getMessage())
                .status(Visit.Status.REQUESTED)
                .build();
        visit = visitRepository.save(visit);

        analyticsEventService.emit("visit_requested", tenantId, "STUDENT", listing.getId(),
                Map.of("visitId", visit.getId().toString()));

        notificationService.createNotification(
                landlord.getId(),
                Notification.NotificationType.VISIT_REQUESTED,
                "New Visit Request",
                tenant.getFirstName() + " " + tenant.getLastName() + " requested to view "
                        + listing.getTitle() + " on " + format(request.getRequestedDatetime()),
                visit.getId(),
                "VISIT",
                "/landlord/visits");

        return enrich(VisitResponse.fromEntity(visit));
    }

    /** Landlord accepts / reschedules / cancels / completes / marks no-show. */
    @Transactional
    public VisitResponse updateVisit(UUID visitId, UUID landlordId, VisitUpdateRequest request) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));

        if (!visit.getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("You do not have permission to update this visit");
        }

        Visit.Status target = request.getStatus();
        boolean confirmed = visit.getStatus() == Visit.Status.ACCEPTED || visit.getStatus() == Visit.Status.RESCHEDULED;

        switch (target) {
            case ACCEPTED -> {
                requireActive(visit);
                visit.setVisitDatetime(request.getVisitDatetime() != null
                        ? request.getVisitDatetime() : visit.getRequestedDatetime());
                visit.setLandlordConfirmedAt(LocalDateTime.now());
            }
            case RESCHEDULED -> {
                requireActive(visit);
                if (request.getVisitDatetime() == null) {
                    throw new BadRequestException("A new date/time is required to reschedule");
                }
                visit.setVisitDatetime(request.getVisitDatetime());
                visit.setRescheduleReason(request.getReason());
            }
            case CANCELLED -> {
                requireActive(visit);
                visit.setCancellationReason(request.getReason());
                visit.setCancelledAt(LocalDateTime.now());
            }
            case COMPLETED -> {
                if (!confirmed) {
                    throw new BadRequestException("Only an accepted visit can be marked completed");
                }
                visit.setCompletedAt(LocalDateTime.now());
            }
            case NO_SHOW -> {
                if (!confirmed) {
                    throw new BadRequestException("Only an accepted visit can be marked no-show");
                }
                visit.setCompletedAt(LocalDateTime.now());
            }
            default -> throw new BadRequestException("Unsupported status: " + target);
        }

        visit.setStatus(target);
        if (request.getResponse() != null) {
            visit.setLandlordResponse(request.getResponse());
        }
        visit = visitRepository.save(visit);

        analyticsEventService.emit("visit_status_changed", landlordId, "LANDLORD", visit.getListing().getId(),
                Map.of("visitId", visit.getId().toString(), "status", target.name()));

        notifyTenantOfUpdate(visit, target);
        return enrich(VisitResponse.fromEntity(visit));
    }

    /** Tenant cancels their own visit. */
    @Transactional
    public VisitResponse cancelByTenant(UUID visitId, UUID tenantId, String reason) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));

        if (!visit.getTenant().getId().equals(tenantId)) {
            throw new BadRequestException("You do not have permission to cancel this visit");
        }
        if (!visit.isActive()) {
            throw new BadRequestException("Cannot cancel a visit that is already finalized");
        }

        visit.setStatus(Visit.Status.CANCELLED);
        visit.setCancellationReason(reason);
        visit.setCancelledAt(LocalDateTime.now());
        visit = visitRepository.save(visit);

        notificationService.createNotification(
                visit.getLandlord().getId(),
                Notification.NotificationType.VISIT_CANCELLED,
                "Visit Cancelled",
                visit.getTenant().getFirstName() + " cancelled the visit to " + visit.getListing().getTitle(),
                visit.getId(),
                "VISIT",
                "/landlord/visits");

        return enrich(VisitResponse.fromEntity(visit));
    }

    @Transactional(readOnly = true)
    public VisitResponse getVisit(UUID visitId, UUID userId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        boolean isTenant = visit.getTenant().getId().equals(userId);
        boolean isLandlord = visit.getLandlord().getId().equals(userId);
        if (!isTenant && !isLandlord) {
            throw new BadRequestException("You do not have permission to view this visit");
        }
        return enrich(VisitResponse.fromEntity(visit));
    }

    @Transactional(readOnly = true)
    public Page<VisitResponse> getTenantVisits(UUID tenantId, Visit.Status status, Pageable pageable) {
        Page<Visit> visits = status != null
                ? visitRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : visitRepository.findByTenantId(tenantId, pageable);
        return visits.map(v -> enrich(VisitResponse.fromEntity(v)));
    }

    @Transactional(readOnly = true)
    public Page<VisitResponse> getLandlordVisits(UUID landlordId, Visit.Status status, Pageable pageable) {
        Page<Visit> visits = status != null
                ? visitRepository.findByLandlordIdAndStatus(landlordId, status, pageable)
                : visitRepository.findByLandlordId(landlordId, pageable);
        return visits.map(v -> enrich(VisitResponse.fromEntity(v)));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTenantStats(UUID tenantId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("requested", visitRepository.countByTenantIdAndStatus(tenantId, Visit.Status.REQUESTED));
        stats.put("accepted", visitRepository.countByTenantIdAndStatus(tenantId, Visit.Status.ACCEPTED));
        stats.put("completed", visitRepository.countByTenantIdAndStatus(tenantId, Visit.Status.COMPLETED));
        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLandlordStats(UUID landlordId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("requested", visitRepository.countByLandlordIdAndStatus(landlordId, Visit.Status.REQUESTED));
        stats.put("accepted", visitRepository.countByLandlordIdAndStatus(landlordId, Visit.Status.ACCEPTED));
        stats.put("completed", visitRepository.countByLandlordIdAndStatus(landlordId, Visit.Status.COMPLETED));
        return stats;
    }

    private void requireActive(Visit visit) {
        if (!visit.isActive()) {
            throw new BadRequestException("Cannot update a visit that is already finalized");
        }
    }

    private void notifyTenantOfUpdate(Visit visit, Visit.Status target) {
        String title;
        String message;
        Notification.NotificationType type;
        String listingTitle = visit.getListing().getTitle();
        switch (target) {
            case ACCEPTED -> {
                type = Notification.NotificationType.VISIT_ACCEPTED;
                title = "Visit Confirmed";
                message = "Your visit to " + listingTitle + " is confirmed for " + format(visit.getVisitDatetime());
            }
            case RESCHEDULED -> {
                type = Notification.NotificationType.VISIT_RESCHEDULED;
                title = "Visit Rescheduled";
                message = "Your visit to " + listingTitle + " was moved to " + format(visit.getVisitDatetime());
            }
            case CANCELLED -> {
                type = Notification.NotificationType.VISIT_CANCELLED;
                title = "Visit Cancelled";
                message = "Your visit to " + listingTitle + " was cancelled by the landlord";
            }
            case COMPLETED -> {
                type = Notification.NotificationType.VISIT_COMPLETED;
                title = "Visit Completed";
                message = "Your visit to " + listingTitle + " is marked completed";
            }
            case NO_SHOW -> {
                type = Notification.NotificationType.VISIT_NO_SHOW;
                title = "Visit Marked No-Show";
                message = "Your visit to " + listingTitle + " was marked as a no-show";
            }
            default -> {
                return;
            }
        }
        notificationService.createNotification(
                visit.getTenant().getId(), type, title, message,
                visit.getId(), "VISIT", "/visits");
    }

    private VisitResponse enrich(VisitResponse response) {
        if (response == null || response.getListingId() == null) {
            return response;
        }
        listingPhotoRepository.findByListingIdAndIsPrimary(response.getListingId(), true)
                .ifPresent(photo -> response.setListingPrimaryPhotoUrl(photo.getPhotoUrl()));
        if (response.getListingPrimaryPhotoUrl() == null) {
            listingPhotoRepository.findFirstByListingIdOrderByDisplayOrderAsc(response.getListingId())
                    .ifPresent(photo -> response.setListingPrimaryPhotoUrl(photo.getPhotoUrl()));
        }
        return response;
    }

    private static String format(LocalDateTime dt) {
        return dt == null ? "the proposed time" : WHEN.format(dt);
    }
}
