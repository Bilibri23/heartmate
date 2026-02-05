package org.rooms.roombuddy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_read", columnList = "user_id, is_read"),
    @Index(name = "idx_notification_created", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "reference_id")
    private UUID referenceId;
    
    @Column(name = "reference_type")
    private String referenceType;
    
    @Column(name = "action_url")
    private String actionUrl;
    
    @Column(name = "is_read")
    @Builder.Default
    private boolean read = false;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum NotificationType {
        // Application notifications
        APPLICATION_RECEIVED,
        APPLICATION_ACCEPTED,
        APPLICATION_REJECTED,
        
        // Lease notifications
        LEASE_CREATED,
        LEASE_SIGNED,
        LEASE_ACTIVE,
        LEASE_COMPLETED,
        LEASE_TERMINATED,
        LEASE_EXPIRING,
        
        // Payment notifications
        PAYMENT_INITIATED,
        PAYMENT_SUBMITTED,
        PAYMENT_VERIFIED,
        PAYMENT_REJECTED,
        PAYMENT_RECEIVED,
        PAYMENT_REMINDER,
        
        // Review notifications
        REVIEW_RECEIVED,
        REVIEW_RESPONSE,
        
        // Dispute notifications
        DISPUTE_CREATED,
        DISPUTE_UPDATED,
        DISPUTE_RESOLVED,
        
        // Message notifications
        NEW_MESSAGE,
        
        // Match notifications
        NEW_MATCH,
        MUTUAL_MATCH,
        
        // Listing notifications
        LISTING_APPROVED,
        LISTING_REJECTED,
        LISTING_EXPIRED,
        
        // Verification notifications
        VERIFICATION_APPROVED,
        VERIFICATION_REJECTED,
        
        // System notifications
        SYSTEM_ANNOUNCEMENT,
        ACCOUNT_UPDATE,
        
        // Co-Application notifications
        CO_APPLICATION_INVITE,
        CO_APPLICATION_ACCEPTED,
        CO_APPLICATION_DECLINED
    }
}
