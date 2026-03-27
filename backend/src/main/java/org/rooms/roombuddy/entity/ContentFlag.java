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

@Entity
@Table(name = "content_flags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentFlag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private PropertyListing listing;
    
    @Column(name = "listing_id", insertable = false, updatable = false)
    private UUID listingId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;
    
    @Column(name = "reporter_id", insertable = false, updatable = false)
    private UUID reporterId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private FlagReason reason;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FlagStatus status = FlagStatus.PENDING;
    
    // Admin review fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    
    @Column(name = "reviewed_by", insertable = false, updatable = false)
    private UUID reviewedById;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(columnDefinition = "TEXT")
    private String adminNotes;
    
    // Automated detection fields
    @Column(name = "suspicion_score")
    private Double suspicionScore;
    
    @Column(name = "is_automated")
    @Builder.Default
    private Boolean isAutomated = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum FlagReason {
        INAPPROPRIATE_CONTENT,
        VIOLENCE,
        FRAUD,
        SPAM,
        COPYRIGHT,
        ILLEGAL_ACTIVITY,
        QUALITY_ISSUES,
        OTHER
    }
    
    public enum FlagStatus {
        PENDING,
        APPROVED,
        REJECTED,
        RESOLVED
    }
}
