package org.rooms.roombuddy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "saved_searches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedSearch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "query")
    private String query;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "neighborhood")
    private String neighborhood;
    
    @Column(name = "property_type")
    private String propertyType;
    
    @Column(name = "min_price")
    private Integer minPrice;
    
    @Column(name = "max_price")
    private Integer maxPrice;
    
    @Column(name = "bedrooms")
    private Integer bedrooms;
    
    @Column(name = "bathrooms")
    private Integer bathrooms;
    
    @ElementCollection
    @CollectionTable(name = "saved_search_amenities", joinColumns = @JoinColumn(name = "saved_search_id"))
    @Column(name = "amenity")
    private List<String> amenities;
    
    @Column(name = "max_distance")
    private Double maxDistance;
    
    @Column(name = "user_lat")
    private Double userLat;
    
    @Column(name = "user_lon")
    private Double userLon;
    
    @Column(name = "available_from")
    private String availableFrom;
    
    @Column(name = "notify_new_listings")
    private Boolean notifyNewListings;
    
    @Column(name = "notify_price_drops")
    private Boolean notifyPriceDrops;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (notifyNewListings == null) notifyNewListings = true;
        if (notifyPriceDrops == null) notifyPriceDrops = false;
    }
}
