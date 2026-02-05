package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for co-application invitations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoApplicationInvitationResponse {
    
    private UUID id;
    private String status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;
    private String declineReason;
    
    // Listing info
    private UUID listingId;
    private String listingTitle;
    private String listingAddress;
    private String listingCity;
    private Integer listingRent;
    private String listingPhotoUrl;
    
    // Inviter info
    private UUID inviterId;
    private String inviterName;
    private String inviterPhotoUrl;
    
    // Invitee info
    private UUID inviteeId;
    private String inviteeName;
    private String inviteePhotoUrl;
    
    // Match info
    private UUID matchId;
    private Integer compatibilityScore;
    
    // Whether current user is the inviter or invitee
    private boolean isInviter;
    private boolean canRespond;
}
