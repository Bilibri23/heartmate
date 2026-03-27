package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.RoomApplication;

/**
 * DTO for landlord to review (accept/reject) an application
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationReviewRequest {
    
    @NotNull(message = "Status is required")
    private RoomApplication.Status status; // ACCEPTED, REJECTED, SHORTLISTED
    
    @Size(max = 500, message = "Response message cannot exceed 500 characters")
    private String response;
    
    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    private String rejectionReason;
}
