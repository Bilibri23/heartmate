package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to invite a matched roommate to apply together for a listing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoApplicationInviteRequest {
    
    @NotNull(message = "Listing ID is required")
    private UUID listingId;
    
    @NotNull(message = "Roommate ID is required")
    private UUID roommateId; // The matched roommate to invite
    
    @NotNull(message = "Match ID is required")
    private UUID matchId; // The mutual match ID
    
    private String message; // Optional message to roommate
    
    private LocalDate proposedMoveInDate;
    
    private Integer proposedLeaseDuration; // in months
}
