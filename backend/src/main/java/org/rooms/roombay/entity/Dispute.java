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
 * Dispute resolution system for conflicts between students and landlords
 */
@Entity
@Table(name = "disputes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by", nullable = false)
    private User openedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "against_user", nullable = false)
    private User againstUser;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeCategory category;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private DisputeStatus status = DisputeStatus.OPEN;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DisputePriority priority = DisputePriority.MEDIUM;
    
    // Evidence URLs (comma-separated)
    @Column(name = "evidence_urls", columnDefinition = "TEXT")
    private String evidenceUrls;
    
    // Admin handling
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    // Resolution
    @Enumerated(EnumType.STRING)
    private DisputeOutcome outcome;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
    
    // Refund if applicable
    @Column(name = "refund_amount")
    private Integer refundAmount;
    
    @Column(name = "refund_approved")
    @Builder.Default
    private Boolean refundApproved = false;
    
    @Column(name = "reference_code", unique = true)
    private String referenceCode;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum DisputeCategory {
        PAYMENT_ISSUE,
        PROPERTY_CONDITION,
        DEPOSIT_REFUND,
        LEASE_VIOLATION,
        HARASSMENT,
        MISREPRESENTATION,
        MAINTENANCE,
        NOISE_COMPLAINT,
        OTHER
    }
    
    public enum DisputeStatus {
        OPEN,
        UNDER_REVIEW,
        AWAITING_RESPONSE,
        MEDIATION,
        RESOLVED,
        CLOSED,
        ESCALATED
    }
    
    public enum DisputePriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
    
    public enum DisputeOutcome {
        RESOLVED_FOR_STUDENT,
        RESOLVED_FOR_LANDLORD,
        MUTUAL_AGREEMENT,
        NO_ACTION,
        PARTIAL_REFUND,
        FULL_REFUND,
        WARNING_ISSUED,
        ACCOUNT_SUSPENDED
    }
    
    @PrePersist
    public void generateReferenceCode() {
        if (this.referenceCode == null) {
            String datePart = java.time.LocalDate.now().toString().replace("-", "");
            String randomPart = String.format("%04d", (int) (Math.random() * 10000));
            this.referenceCode = "DSP-" + datePart + "-" + randomPart;
        }
    }
}
