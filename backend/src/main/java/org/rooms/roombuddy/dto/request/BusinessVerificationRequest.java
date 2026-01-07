package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting business verification.
 * This is optional but provides higher trust level for property managers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessVerificationRequest {
    
    @NotBlank(message = "Business name is required")
    private String businessName;
    
    @NotBlank(message = "Business registration number is required")
    private String businessRegistrationNumber;
    
    @NotBlank(message = "Business registration document is required")
    private String businessRegistrationDocUrl;
    
    private String taxIdNumber; // Optional
}
