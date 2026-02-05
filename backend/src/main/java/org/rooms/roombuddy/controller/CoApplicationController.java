package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.CoApplicationInviteRequest;
import org.rooms.roombuddy.dto.request.RoomApplicationRequest;
import org.rooms.roombuddy.dto.response.CoApplicationInvitationResponse;
import org.rooms.roombuddy.dto.response.RoomApplicationResponse;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.ApplicationService;
import org.rooms.roombuddy.service.CoApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/co-applications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Co-Applications", description = "APIs for managing co-tenant applications with matched roommates")
@SecurityRequirement(name = "bearerAuth")
public class CoApplicationController {
    
    private final CoApplicationService coApplicationService;
    private final ApplicationService applicationService;
    
    /**
     * Invite a matched roommate to apply together for a listing
     */
    @PostMapping("/invite")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Invite roommate to apply together", 
               description = "Send an invitation to your matched roommate to apply together for a listing")
    public ResponseEntity<CoApplicationInvitationResponse> inviteRoommate(
            @Valid @RequestBody CoApplicationInviteRequest request) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} inviting roommate for co-application", userId);
        
        CoApplicationInvitationResponse response = coApplicationService.inviteRoommate(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Accept a co-application invitation
     */
    @PostMapping("/invitations/{invitationId}/accept")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Accept invitation", description = "Accept an invitation to apply together")
    public ResponseEntity<CoApplicationInvitationResponse> acceptInvitation(
            @PathVariable UUID invitationId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} accepting invitation {}", userId, invitationId);
        
        CoApplicationInvitationResponse response = coApplicationService.acceptInvitation(invitationId, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Decline a co-application invitation
     */
    @PostMapping("/invitations/{invitationId}/decline")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Decline invitation", description = "Decline an invitation to apply together")
    public ResponseEntity<CoApplicationInvitationResponse> declineInvitation(
            @PathVariable UUID invitationId,
            @RequestBody(required = false) Map<String, String> body) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        String reason = body != null ? body.get("reason") : null;
        log.info("User {} declining invitation {}", userId, invitationId);
        
        CoApplicationInvitationResponse response = coApplicationService.declineInvitation(invitationId, userId, reason);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel an invitation (inviter only)
     */
    @DeleteMapping("/invitations/{invitationId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Cancel invitation", description = "Cancel a pending invitation (inviter only)")
    public ResponseEntity<Void> cancelInvitation(@PathVariable UUID invitationId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} cancelling invitation {}", userId, invitationId);
        
        coApplicationService.cancelInvitation(invitationId, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get pending invitations received
     */
    @GetMapping("/invitations/received")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get received invitations", description = "Get pending co-application invitations you've received")
    public ResponseEntity<List<CoApplicationInvitationResponse>> getReceivedInvitations() {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        List<CoApplicationInvitationResponse> invitations = coApplicationService.getReceivedInvitations(userId);
        return ResponseEntity.ok(invitations);
    }
    
    /**
     * Get invitations sent
     */
    @GetMapping("/invitations/sent")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get sent invitations", description = "Get co-application invitations you've sent")
    public ResponseEntity<List<CoApplicationInvitationResponse>> getSentInvitations() {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        List<CoApplicationInvitationResponse> invitations = coApplicationService.getSentInvitations(userId);
        return ResponseEntity.ok(invitations);
    }
    
    /**
     * Get count of pending invitations
     */
    @GetMapping("/invitations/count")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get pending invitation count", description = "Get count of pending invitations")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        long count = coApplicationService.getPendingInvitationCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    /**
     * Check if there's an accepted invitation for a listing with a specific roommate
     */
    @GetMapping("/check/{listingId}/{roommateId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Check invitation status", description = "Check if there's an accepted invitation for applying together")
    public ResponseEntity<Map<String, Boolean>> checkInvitationStatus(
            @PathVariable UUID listingId,
            @PathVariable UUID roommateId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        boolean hasAccepted = coApplicationService.hasAcceptedInvitation(listingId, userId, roommateId);
        return ResponseEntity.ok(Map.of("canApplyTogether", hasAccepted));
    }
    
    /**
     * Submit a joint application from an accepted invitation
     */
    @PostMapping("/invitations/{invitationId}/apply")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit joint application", 
               description = "Submit a joint application for a listing after invitation is accepted")
    public ResponseEntity<RoomApplicationResponse> submitCoApplication(
            @PathVariable UUID invitationId,
            @Valid @RequestBody RoomApplicationRequest request) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} submitting co-application from invitation {}", userId, invitationId);
        
        RoomApplicationResponse response = applicationService.createCoApplication(invitationId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Confirm a co-application (by co-applicant)
     */
    @PostMapping("/applications/{applicationId}/confirm")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Confirm co-application", 
               description = "Confirm a joint application as the co-applicant")
    public ResponseEntity<RoomApplicationResponse> confirmCoApplication(
            @PathVariable UUID applicationId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} confirming co-application {}", userId, applicationId);
        
        RoomApplicationResponse response = applicationService.confirmCoApplication(applicationId, userId);
        return ResponseEntity.ok(response);
    }
}
