package org.rooms.roombay.repository;

import org.rooms.roombay.entity.Payment;
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
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByReferenceCode(String referenceCode);
    
    Page<Payment> findByPayerId(UUID payerId, Pageable pageable);
    
    Page<Payment> findByRecipientId(UUID recipientId, Pageable pageable);
    
    Page<Payment> findByLeaseId(UUID leaseId, Pageable pageable);
    
    List<Payment> findByLeaseIdAndStatus(UUID leaseId, Payment.PaymentStatus status);
    
    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'SUBMITTED' ORDER BY p.createdAt ASC")
    Page<Payment> findPendingVerification(Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.payoutStatus = 'PENDING' AND p.status = 'VERIFIED'")
    List<Payment> findPendingPayouts();
    
    @Query("SELECT p FROM Payment p WHERE p.payer.id = :userId OR p.recipient.id = :userId")
    Page<Payment> findByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'VERIFIED' AND p.createdAt >= :startDate")
    Long sumVerifiedPaymentsSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT SUM(p.platformFee) FROM Payment p WHERE p.status = 'VERIFIED' AND p.createdAt >= :startDate")
    Long sumPlatformFeesSince(@Param("startDate") LocalDateTime startDate);
    
    long countByStatus(Payment.PaymentStatus status);
    
    long countByPayerId(UUID payerId);
    
    long countByRecipientId(UUID recipientId);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUBMITTED'")
    long countPendingVerification();

    @Query("""
            SELECT COUNT(p) FROM Payment p
            WHERE p.id <> :paymentId
              AND p.momoTransactionId IS NOT NULL
              AND LOWER(TRIM(p.momoTransactionId)) = LOWER(TRIM(:transactionId))
              AND p.status IN ('SUBMITTED', 'VERIFIED')
            """)
    long countSubmittedOrVerifiedByTransactionIdExcluding(
            @Param("transactionId") String transactionId,
            @Param("paymentId") UUID paymentId
    );

    @Query("""
            SELECT COUNT(p) FROM Payment p
            WHERE p.id <> :paymentId
              AND p.paymentProofUrl IS NOT NULL
              AND TRIM(p.paymentProofUrl) = TRIM(:proofUrl)
              AND p.status IN ('SUBMITTED', 'VERIFIED')
            """)
    long countSubmittedOrVerifiedByProofUrlExcluding(
            @Param("proofUrl") String proofUrl,
            @Param("paymentId") UUID paymentId
    );
    
    boolean existsByLeaseIdAndPaymentTypeAndStatusIn(UUID leaseId, Payment.PaymentType type, List<Payment.PaymentStatus> statuses);
}
