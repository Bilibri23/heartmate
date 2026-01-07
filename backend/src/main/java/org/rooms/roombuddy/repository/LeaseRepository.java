package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.Lease;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaseRepository extends JpaRepository<Lease, UUID> {
    
    Optional<Lease> findByReferenceCode(String referenceCode);
    
    Page<Lease> findByStudentId(UUID studentId, Pageable pageable);
    
    Page<Lease> findByLandlordId(UUID landlordId, Pageable pageable);
    
    Page<Lease> findByStatus(Lease.LeaseStatus status, Pageable pageable);
    
    List<Lease> findByStudentIdAndStatus(UUID studentId, Lease.LeaseStatus status);
    
    List<Lease> findByLandlordIdAndStatus(UUID landlordId, Lease.LeaseStatus status);
    
    @Query("SELECT l FROM Lease l WHERE l.student.id = :userId OR l.landlord.id = :userId")
    Page<Lease> findByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT l FROM Lease l WHERE l.listing.id = :listingId AND l.status = 'ACTIVE'")
    Optional<Lease> findActiveLeaseByListingId(@Param("listingId") UUID listingId);
    
    @Query("SELECT l FROM Lease l WHERE l.endDate <= :date AND l.status = 'ACTIVE'")
    List<Lease> findExpiringLeases(@Param("date") LocalDate date);
    
    @Query("SELECT l FROM Lease l WHERE l.status = 'PENDING_PAYMENT' AND l.createdAt < :cutoffDate")
    List<Lease> findExpiredPendingPayments(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
    
    boolean existsByListingIdAndStudentIdAndStatusIn(UUID listingId, UUID studentId, List<Lease.LeaseStatus> statuses);
    
    long countByStatus(Lease.LeaseStatus status);
    
    long countByLandlordId(UUID landlordId);
    
    long countByStudentId(UUID studentId);
    
    @Query("SELECT COUNT(l) FROM Lease l WHERE l.status = 'ACTIVE'")
    long countActiveLeases();
    
    @Query("SELECT SUM(l.monthlyRent) FROM Lease l WHERE l.status = 'ACTIVE'")
    Long sumActiveMonthlyRent();
}
