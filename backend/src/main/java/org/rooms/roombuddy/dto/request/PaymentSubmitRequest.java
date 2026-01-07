package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.Payment;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSubmitRequest {
    
    @NotNull(message = "Lease ID is required")
    private UUID leaseId;
    
    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;
    
    @NotBlank(message = "Phone number used for payment is required")
    private String momoPhoneNumber;
    
    private String momoTransactionId;
    
    private String paymentProofUrl;
    
    private String payerNotes;
}
