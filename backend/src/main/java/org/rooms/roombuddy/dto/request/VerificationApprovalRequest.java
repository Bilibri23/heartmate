package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationApprovalRequest {
    
    @NotBlank(message = "Status is required")
    private String status; // VERIFIED or REJECTED
    
    private String rejectionReason; // Required if status is REJECTED
}

