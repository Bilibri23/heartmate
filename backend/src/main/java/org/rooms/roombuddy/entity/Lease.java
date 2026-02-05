package org.rooms.roombuddy.entity;

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
 * Represents a formal lease agreement between a student and landlord
 * Created after an application is accepted and payment is confirmed
 */
@Entity
@Table(name = "leases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lease {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private PropertyListing listing;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private RoomApplication application;
    
    // Lease Terms
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "monthly_rent", nullable = false)
    private Integer monthlyRent; // in XAF
    
    @Column(name = "deposit_amount")
    private Integer depositAmount; // in XAF
    
    @Column(name = "agency_fees")
    private Integer agencyFees; // in XAF
    
    @Column(name = "total_initial_payment")
    private Integer totalInitialPayment; // deposit + first month + agency fees
    
    // Status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private LeaseStatus status = LeaseStatus.PENDING_PAYMENT;
    
    // Signatures (timestamp-based acceptance)
    @Column(name = "student_accepted_at")
    private LocalDateTime studentAcceptedAt;
    
    @Column(name = "landlord_accepted_at")
    private LocalDateTime landlordAcceptedAt;
    
    @Column(name = "student_accepted_terms")
    @Builder.Default
    private Boolean studentAcceptedTerms = false;
    
    @Column(name = "landlord_accepted_terms")
    @Builder.Default
    private Boolean landlordAcceptedTerms = false;
    
    // Digital signatures (Base64 encoded image data)
    @Column(name = "student_signature", columnDefinition = "TEXT")
    private String studentSignature;
    
    @Column(name = "landlord_signature", columnDefinition = "TEXT")
    private String landlordSignature;
    
    @Column(name = "student_signature_type")
    private String studentSignatureType; // "drawn" or "typed"
    
    @Column(name = "landlord_signature_type")
    private String landlordSignatureType; // "drawn" or "typed"
    
    // Terms document (HTML or plain text)
    @Column(name = "terms_content", columnDefinition = "TEXT")
    private String termsContent;
    
    // Move-in/Move-out
    @Column(name = "actual_move_in_date")
    private LocalDate actualMoveInDate;
    
    @Column(name = "actual_move_out_date")
    private LocalDate actualMoveOutDate;
    
    // Termination
    @Enumerated(EnumType.STRING)
    @Column(name = "termination_reason")
    private TerminationReason terminationReason;
    
    @Column(name = "termination_notes", columnDefinition = "TEXT")
    private String terminationNotes;
    
    @Column(name = "terminated_by")
    private UUID terminatedBy;
    
    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;
    
    // Reference code for payments
    @Column(name = "reference_code", unique = true)
    private String referenceCode;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum LeaseStatus {
        PENDING_PAYMENT,    // Waiting for initial payment
        PENDING_SIGNATURES, // Payment received, waiting for both parties to accept
        ACTIVE,             // Both signed, lease is active
        COMPLETED,          // Lease ended normally
        TERMINATED,         // Lease ended early
        CANCELLED,          // Cancelled before activation
        DISPUTED            // Under dispute
    }
    
    public enum TerminationReason {
        MUTUAL_AGREEMENT,
        STUDENT_REQUEST,
        LANDLORD_REQUEST,
        NON_PAYMENT,
        LEASE_VIOLATION,
        PROPERTY_ISSUE,
        OTHER
    }
    
    @PrePersist
    public void generateReferenceCode() {
        if (this.referenceCode == null) {
            // Generate a unique reference code: RB-YYYYMMDD-XXXX
            String datePart = java.time.LocalDate.now().toString().replace("-", "");
            String randomPart = String.format("%04d", (int) (Math.random() * 10000));
            this.referenceCode = "RB-" + datePart + "-" + randomPart;
        }
    }
    
    public boolean isBothPartiesAccepted() {
        return Boolean.TRUE.equals(studentAcceptedTerms) && Boolean.TRUE.equals(landlordAcceptedTerms);
    }
    
    public boolean isActive() {
        return status == LeaseStatus.ACTIVE;
    }
    
    public boolean canBeReviewed() {
        // Allow reviewing active, completed, or terminated leases
        return status == LeaseStatus.ACTIVE || status == LeaseStatus.COMPLETED || status == LeaseStatus.TERMINATED;
    }
    
    public int getDurationMonths() {
        return (int) java.time.temporal.ChronoUnit.MONTHS.between(startDate, endDate);
    }
}
