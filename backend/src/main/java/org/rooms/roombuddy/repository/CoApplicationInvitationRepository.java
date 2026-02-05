package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.CoApplicationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoApplicationInvitationRepository extends JpaRepository<CoApplicationInvitation, UUID> {
    
    /**
     * Find pending invitations sent to a user
     */
    @Query("SELECT i FROM CoApplicationInvitation i WHERE i.invitee.id = :userId AND i.status = 'PENDING' ORDER BY i.createdAt DESC")
    List<CoApplicationInvitation> findPendingInvitationsForUser(@Param("userId") UUID userId);
    
    /**
     * Find invitations sent by a user
     */
    @Query("SELECT i FROM CoApplicationInvitation i WHERE i.inviter.id = :userId ORDER BY i.createdAt DESC")
    List<CoApplicationInvitation> findInvitationsSentByUser(@Param("userId") UUID userId);
    
    /**
     * Find invitation by listing and users
     */
    @Query("SELECT i FROM CoApplicationInvitation i WHERE i.listing.id = :listingId AND i.inviter.id = :inviterId AND i.invitee.id = :inviteeId")
    Optional<CoApplicationInvitation> findByListingAndUsers(
        @Param("listingId") UUID listingId,
        @Param("inviterId") UUID inviterId,
        @Param("inviteeId") UUID inviteeId
    );
    
    /**
     * Check if there's already a pending invitation for this listing and pair
     */
    @Query("SELECT COUNT(i) > 0 FROM CoApplicationInvitation i WHERE i.listing.id = :listingId AND ((i.inviter.id = :userId1 AND i.invitee.id = :userId2) OR (i.inviter.id = :userId2 AND i.invitee.id = :userId1)) AND i.status = 'PENDING'")
    boolean existsPendingInvitation(
        @Param("listingId") UUID listingId,
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2
    );
    
    /**
     * Check if there's already a pending OR accepted invitation for this listing and pair
     * (prevents duplicate invitations after one is already in progress)
     */
    @Query("SELECT COUNT(i) > 0 FROM CoApplicationInvitation i WHERE i.listing.id = :listingId AND ((i.inviter.id = :userId1 AND i.invitee.id = :userId2) OR (i.inviter.id = :userId2 AND i.invitee.id = :userId1)) AND i.status IN ('PENDING', 'ACCEPTED')")
    boolean existsActiveInvitation(
        @Param("listingId") UUID listingId,
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2
    );
    
    /**
     * Find accepted invitation for a listing and user pair (to create co-application)
     */
    @Query("SELECT i FROM CoApplicationInvitation i WHERE i.listing.id = :listingId AND ((i.inviter.id = :userId1 AND i.invitee.id = :userId2) OR (i.inviter.id = :userId2 AND i.invitee.id = :userId1)) AND i.status = 'ACCEPTED'")
    Optional<CoApplicationInvitation> findAcceptedInvitation(
        @Param("listingId") UUID listingId,
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2
    );
    
    /**
     * Count pending invitations for a user
     */
    @Query("SELECT COUNT(i) FROM CoApplicationInvitation i WHERE i.invitee.id = :userId AND i.status = 'PENDING' AND i.expiresAt > CURRENT_TIMESTAMP")
    long countPendingInvitations(@Param("userId") UUID userId);
}
