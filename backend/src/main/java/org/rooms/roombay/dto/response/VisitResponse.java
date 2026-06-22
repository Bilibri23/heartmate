package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.Visit;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for visit responses. Listing photo + tenant/landlord contact details that live on
 * separate entities are filled in by the service layer (see VisitService.enrich...).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitResponse {

    private UUID id;

    private UUID listingId;
    private String listingTitle;
    private String listingCity;
    private String listingNeighborhood;
    private String listingPrimaryPhotoUrl;

    private UUID tenantId;
    private String tenantName;
    private String tenantPhone;

    private UUID landlordId;
    private String landlordName;

    private UUID applicationId;

    private LocalDateTime requestedDatetime;
    private LocalDateTime visitDatetime;

    private Visit.Status status;
    private String tenantMessage;
    private String landlordResponse;
    private String rescheduleReason;
    private String cancellationReason;

    private LocalDateTime landlordConfirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Boolean isActive;

    public static VisitResponse fromEntity(Visit visit) {
        if (visit == null) {
            return null;
        }
        VisitResponseBuilder builder = VisitResponse.builder()
                .id(visit.getId())
                .applicationId(visit.getApplication() == null ? null : visit.getApplication().getId())
                .requestedDatetime(visit.getRequestedDatetime())
                .visitDatetime(visit.getVisitDatetime())
                .status(visit.getStatus())
                .tenantMessage(visit.getTenantMessage())
                .landlordResponse(visit.getLandlordResponse())
                .rescheduleReason(visit.getRescheduleReason())
                .cancellationReason(visit.getCancellationReason())
                .landlordConfirmedAt(visit.getLandlordConfirmedAt())
                .cancelledAt(visit.getCancelledAt())
                .completedAt(visit.getCompletedAt())
                .createdAt(visit.getCreatedAt())
                .updatedAt(visit.getUpdatedAt())
                .isActive(visit.isActive());

        if (visit.getListing() != null) {
            builder.listingId(visit.getListing().getId())
                    .listingTitle(visit.getListing().getTitle())
                    .listingCity(visit.getListing().getCity())
                    .listingNeighborhood(visit.getListing().getNeighborhood());
        }
        if (visit.getTenant() != null) {
            builder.tenantId(visit.getTenant().getId())
                    .tenantName(visit.getTenant().getFirstName() + " " + visit.getTenant().getLastName())
                    .tenantPhone(visit.getTenant().getPhone());
        }
        if (visit.getLandlord() != null) {
            builder.landlordId(visit.getLandlord().getId())
                    .landlordName(visit.getLandlord().getFirstName() + " " + visit.getLandlord().getLastName());
        }
        return builder.build();
    }
}
