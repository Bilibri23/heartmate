package org.rooms.roombuddy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks when users view listings (for behavioral recommendations)
 */
@Entity
@Table(name = "listing_views", indexes = {
    @Index(name = "idx_listing_views_user", columnList = "user_id"),
    @Index(name = "idx_listing_views_listing", columnList = "listing_id"),
    @Index(name = "idx_listing_views_created", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingView {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private PropertyListing listing;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds; // How long they viewed (optional)
    
    @Column(name = "source")
    private String source; // search, recommendation, featured, etc.
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
