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
public class ListingApprovalRequest {
    
    @NotBlank(message = "Status is required")
    private String status; // ACTIVE or REJECTED
    
    private String rejectionReason; // Required if status is REJECTED
    
    private Boolean featured; // Optional: mark as featured
}

