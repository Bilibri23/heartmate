package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.PhoneVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhoneVerificationOtpRepository extends JpaRepository<PhoneVerificationOtp, UUID> {
    
    Optional<PhoneVerificationOtp> findByUserIdAndOtpCodeAndVerifiedFalse(UUID userId, String otpCode);
    
    Optional<PhoneVerificationOtp> findFirstByUserIdAndVerifiedFalseOrderByCreatedAtDesc(UUID userId);
    
    @Query("SELECT o FROM PhoneVerificationOtp o WHERE o.user.id = :userId AND o.phoneNumber = :phoneNumber AND o.verified = false AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<PhoneVerificationOtp> findLatestValidOtpByUserIdAndPhone(@Param("userId") UUID userId, @Param("phoneNumber") String phoneNumber, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE PhoneVerificationOtp o SET o.verified = true, o.verifiedAt = :verifiedAt WHERE o.user.id = :userId AND o.verified = false")
    void markAllAsVerifiedForUser(@Param("userId") UUID userId, @Param("verifiedAt") LocalDateTime verifiedAt);
    
    @Modifying
    @Query("DELETE FROM PhoneVerificationOtp o WHERE o.expiresAt < :now OR o.verified = true")
    void deleteExpiredOrVerifiedOtps(@Param("now") LocalDateTime now);
}

