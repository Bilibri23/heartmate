package org.rooms.roombuddy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "listing_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingPreferences {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    // Budget Preferences
    @Column(name = "min_budget")
    private Integer minBudget; // in XAF
    
    @Column(name = "max_budget")
    private Integer maxBudget; // in XAF
    
    // Location Preferences
    @Column(name = "preferred_locations", columnDefinition = "TEXT[]")
    private List<String> preferredLocations;
    
    @Column(name = "max_distance_from_campus", precision = 10, scale = 2)
    private BigDecimal maxDistanceFromCampus; // in kilometers

    // Property type preferences
    @Column(name = "property_types", columnDefinition = "TEXT[]")
    private List<String> propertyTypes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
