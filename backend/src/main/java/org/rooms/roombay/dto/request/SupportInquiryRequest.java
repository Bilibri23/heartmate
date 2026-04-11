package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportInquiryRequest {

    /** Optional: BUG, ACCOUNT, LISTING, TRUST_SAFETY, OTHER */
    @Size(max = 64)
    private String category;

    @NotBlank
    @Size(max = 500)
    private String subject;

    @NotBlank
    @Size(max = 8000)
    private String message;
}
