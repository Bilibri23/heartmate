package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.Visit;

import java.time.LocalDateTime;

/**
 * DTO for a landlord updating a visit: accept, reschedule, cancel, complete, or no-show.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitUpdateRequest {

    @NotNull(message = "Status is required")
    private Visit.Status status; // ACCEPTED, RESCHEDULED, CANCELLED, COMPLETED, NO_SHOW

    /** Required when status is RESCHEDULED; optional confirmation time for ACCEPTED. */
    private LocalDateTime visitDatetime;

    @Size(max = 500, message = "Response cannot exceed 500 characters")
    private String response;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
