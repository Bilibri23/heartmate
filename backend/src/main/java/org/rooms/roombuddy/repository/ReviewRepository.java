package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    
    Page<Review> findByRevieweeIdAndVisibleTrue(UUID revieweeId, Pageable pageable);
    
    Page<Review> findByReviewerId(UUID reviewerId, Pageable pageable);
    
    Page<Review> findByListingIdAndVisibleTrue(UUID listingId, Pageable pageable);
    
    List<Review> findByLeaseId(UUID leaseId);
    
    Optional<Review> findByReviewerIdAndLeaseIdAndReviewType(UUID reviewerId, UUID leaseId, Review.ReviewType reviewType);
    
    boolean existsByReviewerIdAndLeaseIdAndReviewType(UUID reviewerId, UUID leaseId, Review.ReviewType reviewType);
    
    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.reviewee.id = :userId AND r.visible = true")
    Double getAverageRatingForUser(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.listing.id = :listingId AND r.visible = true")
    Double getAverageRatingForListing(@Param("listingId") UUID listingId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewee.id = :userId AND r.visible = true")
    long countReviewsForUser(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.listing.id = :listingId AND r.visible = true")
    long countReviewsForListing(@Param("listingId") UUID listingId);
    
    Page<Review> findByFlaggedTrue(Pageable pageable);
    
    @Query("SELECT r.overallRating, COUNT(r) FROM Review r WHERE r.reviewee.id = :userId AND r.visible = true GROUP BY r.overallRating")
    List<Object[]> getRatingDistributionForUser(@Param("userId") UUID userId);
    
    @Query("SELECT r FROM Review r WHERE r.reviewee.id = :userId AND r.visible = true ORDER BY r.createdAt DESC")
    List<Review> findRecentReviewsForUser(@Param("userId") UUID userId, Pageable pageable);
}
