package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for a tenant requesting a property visit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitRequest {

    @NotNull(message = "Listing is required")
    private UUID listingId;

    /** Optional link to an existing application for timeline continuity. */
    private UUID applicationId;

    @NotNull(message = "Requested date/time is required")
    @FutureOrPresent(message = "Visit date/time must be in the future")
    private LocalDateTime requestedDatetime;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
}
