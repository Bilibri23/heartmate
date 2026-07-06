package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Submit a realtor KYC document (e.g. BUSINESS_REGISTRATION, NATIONAL_ID, AGENCY_LICENSE). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtorVerificationDocumentRequest {

    @NotBlank(message = "Document type is required")
    private String documentType;

    @NotBlank(message = "Document URL is required")
    private String documentUrl;
}
