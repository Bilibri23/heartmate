package org.rooms.roombay.repository;

import org.rooms.roombay.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(String token);

    @Modifying
    @Query("UPDATE EmailVerificationToken e SET e.used = true WHERE e.user.id = :userId AND e.used = false")
    void markAllUsedByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken e WHERE e.expiresAt < :now OR e.used = true")
    void deleteExpiredOrUsed(@Param("now") LocalDateTime now);
}
