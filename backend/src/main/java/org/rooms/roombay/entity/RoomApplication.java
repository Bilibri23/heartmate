package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a student's application to a property listing.
 * Supports both individual and co-tenant (with matched roommate) applications.
 */
@Entity
@Table(name = "room_applications", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "listing_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomApplication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private PropertyListing listing;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student; // Primary applicant
    
    // Co-tenant support
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_applicant_id")
    private User coApplicant; // Matched roommate applying together (optional)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match; // The roommate match (if co-application)
    
    @Column(name = "is_co_application")
    @Builder.Default
    private Boolean isCoApplication = false;
    
    @Column(name = "co_applicant_confirmed")
    @Builder.Default
    private Boolean coApplicantConfirmed = false; // Co-applicant has confirmed
    
    @Column(name = "co_applicant_confirmed_at")
    private LocalDateTime coApplicantConfirmedAt;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // Student's message to landlord
    
    @Column(name = "move_in_date")
    private LocalDate moveInDate; // Desired move-in date
    
    @Column(name = "lease_duration_months")
    private Integer leaseDurationMonths; // How long they want to stay
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private Status status = Status.PENDING;
    
    @Column(name = "landlord_response", columnDefinition = "TEXT")
    private String landlordResponse; // Landlord's response message
    
    @Column(name = "landlord_viewed_at")
    private LocalDateTime landlordViewedAt; // When landlord viewed the application
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy; // Landlord who reviewed
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Application expiration
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum Status {
        PENDING,        // Waiting for landlord review
        VIEWED,         // Landlord has seen it
        SHORTLISTED,    // Landlord is considering
        ACCEPTED,       // Landlord approved
        REJECTED,       // Landlord declined
        WITHDRAWN,      // Student cancelled
        EXPIRED         // Application expired
    }
    
    /**
     * Check if application is still active
     */
    public boolean isActive() {
        return status == Status.PENDING || status == Status.VIEWED || status == Status.SHORTLISTED;
    }
    
    /**
     * Check if application has been reviewed
     */
    public boolean isReviewed() {
        return status == Status.ACCEPTED || status == Status.REJECTED;
    }
    
    /**
     * Mark as viewed by landlord
     */
    public void markAsViewed() {
        if (this.status == Status.PENDING) {
            this.status = Status.VIEWED;
            this.landlordViewedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Check if this is a co-application with a roommate
     */
    public boolean hasCoApplicant() {
        return Boolean.TRUE.equals(isCoApplication) && coApplicant != null;
    }
    
    /**
     * Check if co-application is fully confirmed by both parties
     */
    public boolean isCoApplicationConfirmed() {
        return hasCoApplicant() && Boolean.TRUE.equals(coApplicantConfirmed);
    }
    
    /**
     * Confirm co-applicant participation
     */
    public void confirmCoApplicant() {
        if (hasCoApplicant()) {
            this.coApplicantConfirmed = true;
            this.coApplicantConfirmedAt = LocalDateTime.now();
        }
    }
}
