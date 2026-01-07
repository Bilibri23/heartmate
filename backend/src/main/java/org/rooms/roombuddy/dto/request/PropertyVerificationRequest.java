package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting property ownership verification.
 * This proves the landlord owns or has rights to rent the property.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyVerificationRequest {
    
    @NotBlank(message = "Property ownership document is required")
    private String propertyOwnershipDocUrl; // Land title, rental agreement, etc.
    
    private String utilityBillUrl; // Optional but helps verify address
}
