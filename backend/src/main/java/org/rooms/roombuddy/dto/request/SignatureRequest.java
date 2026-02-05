package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignatureRequest {
    
    @NotBlank(message = "Signature data is required")
    private String signatureData; // Base64 encoded image
    
    @NotBlank(message = "Signature type is required")
    private String signatureType; // "drawn" or "typed"
    
    @NotBlank(message = "Timestamp is required")
    private String timestamp;
    
    @NotBlank(message = "Signer name is required")
    private String signerName;
}
