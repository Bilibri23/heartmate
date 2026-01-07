package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for landlord verification details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandlordVerificationResponse {
    
    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    
    // Identity verification
    private String idType;
    private String idNumber;
    private String idFrontPhotoUrl;
    private String idBackPhotoUrl;
    private String selfieWithIdUrl;
    private String idExpiryDate;
    private String identityStatus;
    private String identityRejectionReason;
    
    // Business verification
    private String businessName;
    private String businessRegistrationNumber;
    private String businessRegistrationDocUrl;
    private String taxIdNumber;
    private String businessStatus;
    private String businessRejectionReason;
    
    // Property verification
    private String propertyOwnershipDocUrl;
    private String utilityBillUrl;
    private String propertyStatus;
    private String propertyRejectionReason;
    
    // Address
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String region;
    private String postalCode;
    
    // Overall status
    private String verificationLevel;
    private Integer trustScore;
    private Boolean isTrustedLandlord;
    
    // Statistics
    private Integer totalListings;
    private Integer successfulRentals;
    private Integer reportedCount;
    
    // Admin info
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private String adminNotes;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
