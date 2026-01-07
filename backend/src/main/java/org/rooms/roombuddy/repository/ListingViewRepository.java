package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.ListingView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListingViewRepository extends JpaRepository<ListingView, UUID> {
    
    /**
     * Get all listings viewed by a user
     */
    @Query("SELECT lv FROM ListingView lv WHERE lv.user.id = :userId ORDER BY lv.createdAt DESC")
    List<ListingView> findByUserId(@Param("userId") UUID userId);
    
    /**
     * Get recent views (last 30 days) for recommendation engine
     */
    @Query("SELECT lv FROM ListingView lv WHERE lv.user.id = :userId " +
           "AND lv.createdAt >= :since ORDER BY lv.createdAt DESC")
    List<ListingView> findRecentByUserId(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    /**
     * Get most viewed listings by a user (for pattern detection)
     */
    @Query("SELECT lv.listing.id, COUNT(lv) as viewCount FROM ListingView lv " +
           "WHERE lv.user.id = :userId " +
           "GROUP BY lv.listing.id " +
           "ORDER BY viewCount DESC")
    List<Object[]> findMostViewedByUser(@Param("userId") UUID userId);
    
    /**
     * Check if user has viewed a specific listing
     */
    @Query("SELECT COUNT(lv) > 0 FROM ListingView lv " +
           "WHERE lv.user.id = :userId AND lv.listing.id = :listingId")
    boolean hasUserViewedListing(@Param("userId") UUID userId, @Param("listingId") UUID listingId);
}
