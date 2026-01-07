package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.RoomApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomApplicationRepository extends JpaRepository<RoomApplication, UUID> {
    
    /**
     * Find all applications for a specific listing
     */
    Page<RoomApplication> findByListingId(UUID listingId, Pageable pageable);
    
    /**
     * Find all applications by a specific student
     */
    Page<RoomApplication> findByStudentId(UUID studentId, Pageable pageable);
    
    /**
     * Find applications for a listing with specific status
     */
    Page<RoomApplication> findByListingIdAndStatus(UUID listingId, RoomApplication.Status status, Pageable pageable);
    
    /**
     * Find applications by student with specific status
     */
    Page<RoomApplication> findByStudentIdAndStatus(UUID studentId, RoomApplication.Status status, Pageable pageable);
    
    /**
     * Find all applications for a landlord's listings
     */
    @Query("SELECT a FROM RoomApplication a WHERE a.listing.landlord.id = :landlordId")
    Page<RoomApplication> findByLandlordId(@Param("landlordId") UUID landlordId, Pageable pageable);
    
    /**
     * Find applications for a landlord's listings with specific status
     */
    @Query("SELECT a FROM RoomApplication a WHERE a.listing.landlord.id = :landlordId AND a.status = :status")
    Page<RoomApplication> findByLandlordIdAndStatus(
        @Param("landlordId") UUID landlordId, 
        @Param("status") RoomApplication.Status status, 
        Pageable pageable
    );
    
    /**
     * Check if student has already applied to a listing
     */
    boolean existsByStudentIdAndListingId(UUID studentId, UUID listingId);
    
    /**
     * Find existing application by student and listing
     */
    Optional<RoomApplication> findByStudentIdAndListingId(UUID studentId, UUID listingId);
    
    /**
     * Count applications for a listing
     */
    long countByListingId(UUID listingId);
    
    /**
     * Count applications by status for a listing
     */
    long countByListingIdAndStatus(UUID listingId, RoomApplication.Status status);
    
    /**
     * Count applications for a student
     */
    long countByStudentId(UUID studentId);
    
    /**
     * Count applications by status for a student
     */
    long countByStudentIdAndStatus(UUID studentId, RoomApplication.Status status);
    
    /**
     * Count applications for all landlord's listings
     */
    @Query("SELECT COUNT(a) FROM RoomApplication a WHERE a.listing.landlord.id = :landlordId")
    long countByLandlordId(@Param("landlordId") UUID landlordId);
    
    /**
     * Count pending applications for a landlord
     */
    @Query("SELECT COUNT(a) FROM RoomApplication a WHERE a.listing.landlord.id = :landlordId AND a.status = :status")
    long countByLandlordIdAndStatus(@Param("landlordId") UUID landlordId, @Param("status") RoomApplication.Status status);
    
    /**
     * Find expired applications
     */
    @Query("SELECT a FROM RoomApplication a WHERE a.expiresAt < :now AND a.status IN ('PENDING', 'VIEWED', 'SHORTLISTED')")
    List<RoomApplication> findExpiredApplications(@Param("now") LocalDateTime now);
    
    /**
     * Get recent applications for a student
     */
    List<RoomApplication> findTop10ByStudentIdOrderByCreatedAtDesc(UUID studentId);
    
    /**
     * Get recent applications for a landlord
     */
    @Query("SELECT a FROM RoomApplication a WHERE a.listing.landlord.id = :landlordId ORDER BY a.createdAt DESC")
    List<RoomApplication> findTop10ByLandlordIdOrderByCreatedAtDesc(@Param("landlordId") UUID landlordId, Pageable pageable);
    
    /**
     * Count pending applications
     */
    long countByStatus(RoomApplication.Status status);
    
    /**
     * Count pending applications specifically
     */
    @Query("SELECT COUNT(a) FROM RoomApplication a WHERE a.status = 'PENDING'")
    long countPendingApplications();
    
    /**
     * Count approved applications
     */
    @Query("SELECT COUNT(a) FROM RoomApplication a WHERE a.status = 'APPROVED'")
    long countApprovedApplications();
    
    /**
     * Count distinct users with applications
     */
    @Query("SELECT COUNT(DISTINCT a.student.id) FROM RoomApplication a")
    long countUsersWithApplications();
}
