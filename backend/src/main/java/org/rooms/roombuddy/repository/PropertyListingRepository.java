package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.PropertyListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyListingRepository extends JpaRepository<PropertyListing, UUID> {
    
    List<PropertyListing> findByLandlordId(UUID landlordId);
    
    List<PropertyListing> findByStatus(PropertyListing.Status status);
    
    List<PropertyListing> findByStatusAndVerified(PropertyListing.Status status, Boolean verified);
    
    @Query("SELECT l FROM PropertyListing l WHERE l.status = 'ACTIVE' AND l.verified = true ORDER BY l.createdAt DESC")
    List<PropertyListing> findActiveVerifiedListings();
    
    @Query("SELECT l FROM PropertyListing l WHERE l.status = 'ACTIVE' AND l.verified = true AND l.featured = true ORDER BY l.createdAt DESC")
    List<PropertyListing> findFeaturedListings();
    
    @Query("SELECT l FROM PropertyListing l WHERE l.status = 'PENDING' ORDER BY l.createdAt ASC")
    List<PropertyListing> findPendingListings();
    
    long countByLandlordId(UUID landlordId);
    
    // Analytics methods
    @Query("SELECT COUNT(l) FROM PropertyListing l WHERE l.status != 'DELETED'")
    long countByDeletedFalse();
    
    @Query("SELECT COUNT(l) FROM PropertyListing l WHERE l.status = 'DELETED'")
    long countByDeletedTrue();
    
    long countByCreatedAtAfter(LocalDateTime date);
    
    @Query("SELECT l.city, COUNT(l) FROM PropertyListing l WHERE l.status != 'DELETED' GROUP BY l.city ORDER BY COUNT(l) DESC")
    List<Object[]> findTopCitiesByListingCount();
    
    @Query("SELECT COUNT(DISTINCT l.landlord.id) FROM PropertyListing l")
    long countDistinctLandlords();
    
    /**
     * Get top listings by views count (for popular recommendations)
     */
    @Query(value = "SELECT * FROM property_listings WHERE status = 'ACTIVE' AND verified = true " +
           "ORDER BY views_count DESC, created_at DESC LIMIT :limit", nativeQuery = true)
    List<PropertyListing> findTopByViewsCount(@Param("limit") int limit);
}

