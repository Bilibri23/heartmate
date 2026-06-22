package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a property viewing/visit a tenant requests and a landlord manages.
 * Mirrors the {@link RoomApplication} pattern. A visit may optionally be linked to an
 * application (nullable) so the application timeline can reflect viewing progress.
 */
@Entity
@Table(name = "visits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private PropertyListing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    // Denormalized from listing.landlord for symmetric tenant/landlord queries.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    // Optional link to an application (a visit can also be requested while just browsing).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private RoomApplication application;

    @Column(name = "requested_datetime", nullable = false)
    private LocalDateTime requestedDatetime; // When the tenant wants to visit

    @Column(name = "visit_datetime")
    private LocalDateTime visitDatetime; // Confirmed time (null until ACCEPTED/RESCHEDULED)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private Status status = Status.REQUESTED;

    @Column(name = "tenant_message", columnDefinition = "TEXT")
    private String tenantMessage;

    @Column(name = "landlord_response", columnDefinition = "TEXT")
    private String landlordResponse;

    @Column(name = "reschedule_reason", columnDefinition = "TEXT")
    private String rescheduleReason;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "landlord_confirmed_at")
    private LocalDateTime landlordConfirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        REQUESTED,    // Tenant asked, awaiting landlord
        ACCEPTED,     // Landlord confirmed the time
        RESCHEDULED,  // Landlord proposed a new time
        COMPLETED,    // Visit happened
        CANCELLED,    // Cancelled by tenant or landlord
        NO_SHOW       // Tenant did not show up
    }

    /** Open visits a landlord can still act on or a tenant can still cancel. */
    public boolean isActive() {
        return status == Status.REQUESTED || status == Status.ACCEPTED || status == Status.RESCHEDULED;
    }
}
