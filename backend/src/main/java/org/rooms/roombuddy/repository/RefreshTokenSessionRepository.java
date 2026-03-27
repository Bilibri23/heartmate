package org.rooms.roombay.repository;

import org.rooms.roombay.entity.RefreshTokenSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, UUID> {
    Optional<RefreshTokenSession> findByTokenId(UUID tokenId);

    @Modifying
    @Query("UPDATE RefreshTokenSession r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
    void revokeAllActiveByUserId(@Param("userId") UUID userId);
}
