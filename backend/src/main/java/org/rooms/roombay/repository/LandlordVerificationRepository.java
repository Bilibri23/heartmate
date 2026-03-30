package org.rooms.roombay.repository;

import org.rooms.roombay.entity.LandlordVerification;
import org.rooms.roombay.entity.LandlordVerification.VerificationLevel;
import org.rooms.roombay.entity.LandlordVerification.VerificationStatus;
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
public interface LandlordVerificationRepository extends JpaRepository<LandlordVerification, UUID> {
    
    Optional<LandlordVerification> findByUserId(UUID userId);
    
    boolean existsByUserId(UUID userId);
    
    void deleteByUserId(UUID userId);
    
    // Find by verification level
    List<LandlordVerification> findByVerificationLevel(VerificationLevel level);
    
    // Find pending identity verifications
    List<LandlordVerification> findByIdentityStatus(VerificationStatus status);
    
    // Find pending business verifications
    List<LandlordVerification> findByBusinessStatus(VerificationStatus status);
    
    // Find pending property verifications
    List<LandlordVerification> findByPropertyStatus(VerificationStatus status);
    
    // Find all pending verifications (any type)
    @Query("SELECT lv FROM LandlordVerification lv WHERE " +
           "lv.identityStatus = 'PENDING' OR " +
           "lv.businessStatus = 'PENDING' OR " +
           "lv.propertyStatus = 'PENDING'")
    List<LandlordVerification> findAllPendingVerifications();
    
    // Paginated version
    @Query("SELECT lv FROM LandlordVerification lv WHERE " +
           "lv.identityStatus = 'PENDING' OR " +
           "lv.businessStatus = 'PENDING' OR " +
           "lv.propertyStatus = 'PENDING'")
    Page<LandlordVerification> findAllPendingVerifications(Pageable pageable);
    
    // Find trusted landlords
    List<LandlordVerification> findByIsTrustedLandlordTrue();
    
    // Find by trust score range
    @Query("SELECT lv FROM LandlordVerification lv WHERE lv.trustScore >= :minScore")
    List<LandlordVerification> findByTrustScoreGreaterThanEqual(@Param("minScore") Integer minScore);
    
    // Count by verification status
    long countByIdentityStatus(VerificationStatus status);
    long countByBusinessStatus(VerificationStatus status);
    long countByPropertyStatus(VerificationStatus status);
    
    // Count trusted landlords
    long countByIsTrustedLandlordTrue();
    
    // Find landlords with reports
    @Query("SELECT lv FROM LandlordVerification lv WHERE lv.reportedCount > 0 ORDER BY lv.reportedCount DESC")
    List<LandlordVerification> findReportedLandlords();
}
