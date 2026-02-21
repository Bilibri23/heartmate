package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.ContentFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentFlagRepository extends JpaRepository<ContentFlag, UUID> {
    
    // Find flags by listing
    List<ContentFlag> findByListingIdOrderByCreatedAtDesc(UUID listingId);
    
    // Find pending flags by listing
    List<ContentFlag> findByListingIdAndStatusOrderByCreatedAtDesc(UUID listingId, ContentFlag.FlagStatus status);
    
    // Count pending flags for a listing
    long countByListingIdAndStatus(UUID listingId, ContentFlag.FlagStatus status);
    
    // Check if user already flagged content
    boolean existsByListingIdAndReporterId(UUID listingId, UUID reporterId);
    
    // Find flags by reporter
    List<ContentFlag> findByReporterIdOrderByCreatedAtDesc(UUID reporterId);
    
    // Find all pending flags (for admin dashboard)
    @Query("SELECT f FROM ContentFlag f WHERE f.status = 'PENDING' ORDER BY f.createdAt DESC")
    List<ContentFlag> findAllPendingFlags();
    
    // Find automated flags
    @Query("SELECT f FROM ContentFlag f WHERE f.isAutomated = true ORDER BY f.createdAt DESC")
    List<ContentFlag> findAutomatedFlags();
    
    // Find high-priority flags (high suspicion score)
    @Query("SELECT f FROM ContentFlag f WHERE f.suspicionScore > 0.7 AND f.status = 'PENDING' ORDER BY f.suspicionScore DESC")
    List<ContentFlag> findHighPriorityFlags();
    
    // Count flags by reason
    @Query("SELECT f.reason, COUNT(f) FROM ContentFlag f WHERE f.status = 'PENDING' GROUP BY f.reason")
    List<Object[]> countFlagsByReason();
    
    // Find recently flagged content
    @Query("SELECT f FROM ContentFlag f WHERE f.createdAt >= :since ORDER BY f.createdAt DESC")
    List<ContentFlag> findRecentFlags(@Param("since") java.time.LocalDateTime since);
    
    // Find flags requiring immediate attention
    @Query("SELECT f FROM ContentFlag f WHERE " +
           "(f.suspicionScore > 0.8 OR f.reason = 'ILLEGAL_ACTIVITY' OR f.reason = 'VIOLENCE') " +
           "AND f.status = 'PENDING' " +
           "ORDER BY f.suspicionScore DESC, f.createdAt DESC")
    List<ContentFlag> findUrgentFlags();
}
