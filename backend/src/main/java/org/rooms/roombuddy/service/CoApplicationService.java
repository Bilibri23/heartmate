package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.CoApplicationInviteRequest;
import org.rooms.roombay.dto.response.CoApplicationInvitationResponse;
import org.rooms.roombay.entity.*;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing co-application invitations between matched roommates
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CoApplicationService {
    
    private final CoApplicationInvitationRepository invitationRepository;
    private final PropertyListingRepository listingRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final ProfileRepository profileRepository;
    private final RoomApplicationRepository applicationRepository;
    private final ListingPhotoRepository listingPhotoRepository;
    private final NotificationService notificationService;
    
    /**
     * Invite a matched roommate to apply together for a listing
     */
    public CoApplicationInvitationResponse inviteRoommate(UUID inviterId, CoApplicationInviteRequest request) {
        log.info("User {} inviting roommate {} for listing {}", inviterId, request.getRoommateId(), request.getListingId());
        
        // Validate inviter
        User inviter = userRepository.findById(inviterId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Validate invitee (roommate)
        User invitee = userRepository.findById(request.getRoommateId())
            .orElseThrow(() -> new ResourceNotFoundException("Roommate not found"));
        
        // Validate listing
        PropertyListing listing = listingRepository.findById(request.getListingId())
            .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        
        if (listing.getStatus() != PropertyListing.Status.ACTIVE) {
            throw new BadRequestException("Listing is not available for applications");
        }
        
        // Validate match exists and is mutual
        Match match = matchRepository.findById(request.getMatchId())
            .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        
        boolean isValidMatch = (match.getUser1().getId().equals(inviterId) && match.getUser2().getId().equals(request.getRoommateId())) ||
                              (match.getUser2().getId().equals(inviterId) && match.getUser1().getId().equals(request.getRoommateId()));
        
        if (!isValidMatch) {
            throw new BadRequestException("Invalid match for these users");
        }
        
        if (!match.isMutualMatch()) {
            throw new BadRequestException("Both users must have accepted the match to apply together");
        }
        
        // Check if invitation already exists (pending or accepted)
        if (invitationRepository.existsActiveInvitation(request.getListingId(), inviterId, request.getRoommateId())) {
            throw new BadRequestException("An invitation for this listing already exists between you two");
        }
        
        // Check if either user already has an application for this listing
        if (applicationRepository.existsByStudentIdAndListingId(inviterId, request.getListingId())) {
            throw new BadRequestException("You already have an application for this listing");
        }
        if (applicationRepository.existsByStudentIdAndListingId(request.getRoommateId(), request.getListingId())) {
            throw new BadRequestException("Your roommate already has an application for this listing");
        }
        
        // Create invitation
        CoApplicationInvitation invitation = CoApplicationInvitation.builder()
            .listing(listing)
            .inviter(inviter)
            .invitee(invitee)
            .match(match)
            .message(request.getMessage())
            .status(CoApplicationInvitation.Status.PENDING)
            .build();
        
        invitation = invitationRepository.save(invitation);
        
        // Send notification to invitee
        String inviterName = inviter.getFirstName() + " " + inviter.getLastName();
        notificationService.createNotification(
            invitee.getId(),
            Notification.NotificationType.CO_APPLICATION_INVITE,
            "Co-Application Invitation",
            inviterName + " wants to apply together for " + listing.getTitle(),
            invitation.getId(),
            "CO_APPLICATION_INVITATION",
            "/applications/invitations"
        );
        
        log.info("Created co-application invitation {}", invitation.getId());
        return mapToResponse(invitation, inviterId);
    }
    
    /**
     * Accept a co-application invitation
     */
    public CoApplicationInvitationResponse acceptInvitation(UUID invitationId, UUID userId) {
        log.info("User {} accepting invitation {}", userId, invitationId);
        
        CoApplicationInvitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new BadRequestException("You cannot accept this invitation");
        }
        
        if (!invitation.canBeAccepted()) {
            throw new BadRequestException("This invitation cannot be accepted (expired or already responded)");
        }
        
        // Accept the invitation
        invitation.setStatus(CoApplicationInvitation.Status.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation = invitationRepository.save(invitation);
        
        // Notify the inviter
        String inviteeName = invitation.getInvitee().getFirstName() + " " + invitation.getInvitee().getLastName();
        notificationService.createNotification(
            invitation.getInviter().getId(),
            Notification.NotificationType.CO_APPLICATION_ACCEPTED,
            "Invitation Accepted!",
            inviteeName + " agreed to apply together for " + invitation.getListing().getTitle(),
            invitation.getId(),
            "CO_APPLICATION_INVITATION",
            "/listings/" + invitation.getListing().getId()
        );
        
        log.info("Invitation {} accepted", invitationId);
        return mapToResponse(invitation, userId);
    }
    
    /**
     * Decline a co-application invitation
     */
    public CoApplicationInvitationResponse declineInvitation(UUID invitationId, UUID userId, String reason) {
        log.info("User {} declining invitation {}", userId, invitationId);
        
        CoApplicationInvitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new BadRequestException("You cannot decline this invitation");
        }
        
        if (invitation.getStatus() != CoApplicationInvitation.Status.PENDING) {
            throw new BadRequestException("This invitation has already been responded to");
        }
        
        invitation.setStatus(CoApplicationInvitation.Status.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation.setDeclineReason(reason);
        invitation = invitationRepository.save(invitation);
        
        // Notify the inviter
        String inviteeName2 = invitation.getInvitee().getFirstName() + " " + invitation.getInvitee().getLastName();
        notificationService.createNotification(
            invitation.getInviter().getId(),
            Notification.NotificationType.CO_APPLICATION_DECLINED,
            "Invitation Declined",
            inviteeName2 + " declined to apply together for " + invitation.getListing().getTitle(),
            invitation.getId(),
            "CO_APPLICATION_INVITATION",
            "/applications/invitations"
        );
        
        log.info("Invitation {} declined", invitationId);
        return mapToResponse(invitation, userId);
    }
    
    /**
     * Cancel an invitation (inviter only)
     */
    public void cancelInvitation(UUID invitationId, UUID userId) {
        log.info("User {} cancelling invitation {}", userId, invitationId);
        
        CoApplicationInvitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        
        if (!invitation.getInviter().getId().equals(userId)) {
            throw new BadRequestException("Only the inviter can cancel this invitation");
        }
        
        if (invitation.getStatus() != CoApplicationInvitation.Status.PENDING) {
            throw new BadRequestException("This invitation can no longer be cancelled");
        }
        
        invitation.setStatus(CoApplicationInvitation.Status.CANCELLED);
        invitationRepository.save(invitation);
        
        log.info("Invitation {} cancelled", invitationId);
    }
    
    /**
     * Get pending invitations received by user
     */
    public List<CoApplicationInvitationResponse> getReceivedInvitations(UUID userId) {
        return invitationRepository.findPendingInvitationsForUser(userId)
            .stream()
            .filter(i -> !i.isExpired())
            .map(i -> mapToResponse(i, userId))
            .collect(Collectors.toList());
    }
    
    /**
     * Get invitations sent by user
     */
    public List<CoApplicationInvitationResponse> getSentInvitations(UUID userId) {
        return invitationRepository.findInvitationsSentByUser(userId)
            .stream()
            .map(i -> mapToResponse(i, userId))
            .collect(Collectors.toList());
    }
    
    /**
     * Get count of pending invitations for user
     */
    public long getPendingInvitationCount(UUID userId) {
        return invitationRepository.countPendingInvitations(userId);
    }
    
    /**
     * Check if there's an accepted invitation for a listing between two users
     */
    public boolean hasAcceptedInvitation(UUID listingId, UUID userId1, UUID userId2) {
        return invitationRepository.findAcceptedInvitation(listingId, userId1, userId2).isPresent();
    }
    
    /**
     * Get the accepted invitation for co-application creation
     */
    public CoApplicationInvitation getAcceptedInvitation(UUID listingId, UUID userId1, UUID userId2) {
        return invitationRepository.findAcceptedInvitation(listingId, userId1, userId2)
            .orElseThrow(() -> new ResourceNotFoundException("No accepted invitation found"));
    }
    
    private CoApplicationInvitationResponse mapToResponse(CoApplicationInvitation invitation, UUID currentUserId) {
        PropertyListing listing = invitation.getListing();
        User inviter = invitation.getInviter();
        User invitee = invitation.getInvitee();
        
        // Get primary photo
        String listingPhotoUrl = listingPhotoRepository
            .findByListingIdAndIsPrimary(listing.getId(), true)
            .map(p -> p.getPhotoUrl())
            .orElse(null);
        
        // Get profile photos
        String inviterPhotoUrl = profileRepository.findByUserId(inviter.getId())
            .map(Profile::getProfilePhotoUrl)
            .orElse(null);
        String inviteePhotoUrl = profileRepository.findByUserId(invitee.getId())
            .map(Profile::getProfilePhotoUrl)
            .orElse(null);
        
        boolean isInviter = inviter.getId().equals(currentUserId);
        
        return CoApplicationInvitationResponse.builder()
            .id(invitation.getId())
            .status(invitation.getStatus().name())
            .message(invitation.getMessage())
            .createdAt(invitation.getCreatedAt())
            .expiresAt(invitation.getExpiresAt())
            .respondedAt(invitation.getRespondedAt())
            .declineReason(invitation.getDeclineReason())
            .listingId(listing.getId())
            .listingTitle(listing.getTitle())
            .listingAddress(listing.getAddress())
            .listingCity(listing.getCity())
            .listingRent(listing.getRentAmount())
            .listingPhotoUrl(listingPhotoUrl)
            .inviterId(inviter.getId())
            .inviterName(inviter.getFirstName() + " " + inviter.getLastName())
            .inviterPhotoUrl(inviterPhotoUrl)
            .inviteeId(invitee.getId())
            .inviteeName(invitee.getFirstName() + " " + invitee.getLastName())
            .inviteePhotoUrl(inviteePhotoUrl)
            .matchId(invitation.getMatch().getId())
            .compatibilityScore(invitation.getMatch().getCompatibilityScore())
            .isInviter(isInviter)
            .canRespond(!isInviter && invitation.canBeAccepted())
            .build();
    }
}
