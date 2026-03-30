package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin to approve/reject landlord verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandlordVerificationApprovalRequest {
    
    @NotBlank(message = "Verification type is required")
    private String verificationType; // IDENTITY, BUSINESS, PROPERTY
    
    @NotBlank(message = "Status is required")
    private String status; // VERIFIED or REJECTED
    
    private String rejectionReason; // Required if status is REJECTED
    
    private String adminNotes; // Optional notes for internal use
}
