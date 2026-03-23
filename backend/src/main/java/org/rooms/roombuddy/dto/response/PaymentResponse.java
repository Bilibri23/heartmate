package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.Payment;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private UUID id;
    private String referenceCode;
    
    // Lease info
    private UUID leaseId;
    private String leaseReferenceCode;
    private String listingTitle;
    
    // Parties
    private UUID payerId;
    private String payerName;
    private String payerEmail;
    private UUID recipientId;
    private String recipientName;
    
    // Payment details
    private Integer amount;
    private Payment.PaymentType paymentType;
    private Payment.PaymentMethod paymentMethod;
    private String momoPhoneNumber;
    private String momoTransactionId;
    private String momoReference;
    
    // Status
    private Payment.PaymentStatus status;
    private String paymentProofUrl;
    private String payerNotes;
    
    // Verification
    private LocalDateTime verifiedAt;
    private String verificationNotes;
    private String rejectionReason;
    
    // Fees
    private Integer platformFee;
    private Integer landlordPayout;
    
    // Payout
    private Payment.PayoutStatus payoutStatus;
    private LocalDateTime payoutAt;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Instructions for payment (for pending payments)
    private PaymentInstructions paymentInstructions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInstructions {
        private String mtnMomoNumber;
        private String orangeMoneyNumber;
        private String recipientName;
        private String referenceToUse;
        private Integer amountToPay;
        private String instructions;
    }
}
