package org.rooms.roombay.repository;

import org.rooms.roombay.entity.PropertyListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyListingRepository extends JpaRepository<PropertyListing, UUID>, JpaSpecificationExecutor<PropertyListing> {
    
    List<PropertyListing> findByLandlordId(UUID landlordId);
    
    List<PropertyListing> findByStatus(PropertyListing.Status status);

    long countByStatus(PropertyListing.Status status);
    
    List<PropertyListing> findByStatusAndVerified(PropertyListing.Status status, Boolean verified);
    
    @Query("SELECT l FROM PropertyListing l WHERE l.status = 'ACTIVE' AND l.verified = true ORDER BY l.createdAt DESC")
    List<PropertyListing> findActiveVerifiedListings();
    
    @Query("SELECT l FROM PropertyListing l WHERE l.status = 'ACTIVE' AND l.verified = true AND l.featured = true ORDER BY l.createdAt DESC")
    List<PropertyListing> findFeaturedListings();
    
    @Query("SELECT l FROM PropertyListing l WHERE l.status = 'PENDING' ORDER BY l.createdAt ASC")
    List<PropertyListing> findPendingListings();

    @Query("SELECT l FROM PropertyListing l WHERE l.status = 'PENDING' ORDER BY l.createdAt ASC")
    List<PropertyListing> findPendingListings(Pageable pageable);
    
    long countByLandlordId(UUID landlordId);

    long countByLandlordIdAndVerifiedTrue(UUID landlordId);

    @Query("""
            SELECT l FROM PropertyListing l
            WHERE l.landlord.id = :landlordId
              AND l.title = :title
              AND l.rentAmount = :rentAmount
              AND COALESCE(l.city, '') = COALESCE(:city, '')
              AND COALESCE(l.neighborhood, '') = COALESCE(:neighborhood, '')
              AND COALESCE(l.address, '') = COALESCE(:address, '')
              AND l.status IN ('DRAFT', 'PENDING')
              AND l.createdAt >= :createdAfter
            ORDER BY l.createdAt DESC
            """)
    List<PropertyListing> findRecentEquivalentDraftOrPending(
            @Param("landlordId") UUID landlordId,
            @Param("title") String title,
            @Param("rentAmount") Integer rentAmount,
            @Param("city") String city,
            @Param("neighborhood") String neighborhood,
            @Param("address") String address,
            @Param("createdAfter") LocalDateTime createdAfter);
    
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
    @Query(value = "SELECT * FROM property_listings WHERE status = 'ACTIVE' " +
           "ORDER BY views_count DESC, created_at DESC LIMIT :limit", nativeQuery = true)
    List<PropertyListing> findTopByViewsCount(@Param("limit") int limit);

    @Query("SELECT l FROM PropertyListing l WHERE l.status = 'ACTIVE' ORDER BY l.createdAt DESC")
    Page<PropertyListing> findActiveOrderByRecent(Pageable pageable);

    @Query("SELECT l FROM PropertyListing l WHERE l.status = 'ACTIVE' ORDER BY l.viewsCount DESC, l.createdAt DESC")
    Page<PropertyListing> findActiveOrderByTrending(Pageable pageable);

    @Query("SELECT l FROM PropertyListing l WHERE l.status = 'ACTIVE' " +
           "AND ((l.videoTourUrl IS NOT NULL AND TRIM(l.videoTourUrl) <> '') " +
           "OR (l.videoTourEmbedCode IS NOT NULL AND TRIM(l.videoTourEmbedCode) <> '')) " +
           "ORDER BY l.viewsCount DESC, l.createdAt DESC")
    Page<PropertyListing> findActiveWithVideoTour(Pageable pageable);

    @Query("SELECT COUNT(l) FROM PropertyListing l WHERE l.status = 'ACTIVE' " +
           "AND ((l.videoTourUrl IS NOT NULL AND TRIM(l.videoTourUrl) <> '') " +
           "OR (l.videoTourEmbedCode IS NOT NULL AND TRIM(l.videoTourEmbedCode) <> '')) ")
    long countActiveWithVideoTour();
}
