package org.rooms.roombuddy.entity;

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
 * Represents an invitation from one user to their matched roommate
 * to apply together for a property listing as co-tenants.
 */
@Entity
@Table(name = "co_application_invitations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"listing_id", "inviter_id", "invitee_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoApplicationInvitation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private PropertyListing listing;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter; // The user who is inviting their roommate
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee; // The matched roommate being invited
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match; // The mutual match between inviter and invitee
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // Optional message from inviter
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private Status status = Status.PENDING;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum Status {
        PENDING,    // Waiting for invitee response
        ACCEPTED,   // Invitee agreed to apply together
        DECLINED,   // Invitee declined
        EXPIRED,    // Invitation expired
        CANCELLED,  // Inviter cancelled
        APPLIED     // Joint application submitted
    }
    
    @PrePersist
    public void setDefaultExpiry() {
        if (this.expiresAt == null) {
            // Default expiry: 7 days from creation
            this.expiresAt = LocalDateTime.now().plusDays(7);
        }
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean canBeAccepted() {
        return status == Status.PENDING && !isExpired();
    }
}
