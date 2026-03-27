package org.rooms.roombay.repository;

import org.rooms.roombay.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    
    // Find matches where user is user1 or user2
    @Query("SELECT m FROM Match m WHERE (m.user1.id = :userId OR m.user2.id = :userId) AND m.status = :status")
    List<Match> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") Match.Status status);
    
    @Query("SELECT m FROM Match m WHERE (m.user1.id = :userId OR m.user2.id = :userId)")
    List<Match> findByUserId(@Param("userId") UUID userId);
    
    // Find pending matches for a user
    @Query("SELECT m FROM Match m WHERE (m.user1.id = :userId OR m.user2.id = :userId) AND m.status = 'PENDING' ORDER BY m.compatibilityScore DESC")
    List<Match> findPendingMatchesByUserId(@Param("userId") UUID userId);
    
    // Find match between two users
    @Query("SELECT m FROM Match m WHERE (m.user1.id = :user1Id AND m.user2.id = :user2Id) OR (m.user1.id = :user2Id AND m.user2.id = :user1Id)")
    Optional<Match> findMatchBetweenUsers(@Param("user1Id") UUID user1Id, @Param("user2Id") UUID user2Id);
    
    // Check if match exists between two users (using query instead of method name)
    @Query("SELECT COUNT(m) > 0 FROM Match m WHERE (m.user1.id = :user1Id AND m.user2.id = :user2Id) OR (m.user1.id = :user2Id AND m.user2.id = :user1Id)")
    boolean existsMatchBetweenUsers(@Param("user1Id") UUID user1Id, @Param("user2Id") UUID user2Id);
    
    long countByStatus(Match.Status status);
    
    // Count mutual matches
    @Query("SELECT COUNT(m) FROM Match m WHERE m.status = 'MATCHED'")
    long countMutualMatches();
    
    // Count pending matches
    @Query("SELECT COUNT(m) FROM Match m WHERE m.status = 'PENDING'")
    long countPendingMatches();
    
    // Count distinct users with matches
    @Query("SELECT COUNT(DISTINCT m.user1.id) + COUNT(DISTINCT m.user2.id) FROM Match m")
    long countUsersWithMatches();
}

