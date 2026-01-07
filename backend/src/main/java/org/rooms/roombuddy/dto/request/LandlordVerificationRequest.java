package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting landlord identity verification (KYC).
 * This is the first step in the verification process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandlordVerificationRequest {
    
    // ==================== IDENTITY VERIFICATION ====================
    
    @NotBlank(message = "ID document type is required")
    private String idType; // NATIONAL_ID, PASSPORT, DRIVERS_LICENSE, VOTER_CARD, RESIDENCE_PERMIT
    
    @NotBlank(message = "ID number is required")
    private String idNumber;
    
    @NotBlank(message = "Front photo of ID is required")
    private String idFrontPhotoUrl;
    
    private String idBackPhotoUrl; // Optional for some ID types
    
    @NotBlank(message = "Selfie with ID is required for verification")
    private String selfieWithIdUrl;
    
    private String idExpiryDate;
    
    // ==================== ADDRESS VERIFICATION ====================
    
    @NotBlank(message = "Address is required")
    private String addressLine1;
    
    private String addressLine2;
    
    @NotBlank(message = "City is required")
    private String city;
    
    @NotBlank(message = "Region is required")
    private String region;
    
    private String postalCode;
}
