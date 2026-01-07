package org.rooms.roombuddy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a payment transaction (Mobile Money focused for Cameroon)
 * Supports manual verification flow for MTN/Orange Money
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id")
    private Lease lease;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer; // Student making payment
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient; // Landlord receiving payment
    
    // Payment Details
    @Column(nullable = false)
    private Integer amount; // in XAF
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;
    
    // Mobile Money specific fields
    @Column(name = "momo_phone_number")
    private String momoPhoneNumber; // Phone number used for payment
    
    @Column(name = "momo_transaction_id")
    private String momoTransactionId; // Transaction ID from MTN/Orange
    
    @Column(name = "momo_reference")
    private String momoReference; // Reference code shown to user
    
    // Proof of payment (for manual verification)
    @Column(name = "payment_proof_url")
    private String paymentProofUrl; // Screenshot or receipt URL
    
    @Column(name = "payer_notes", columnDefinition = "TEXT")
    private String payerNotes; // Notes from payer
    
    // Status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    // Verification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy; // Admin who verified
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    // Platform fee (commission)
    @Column(name = "platform_fee")
    private Integer platformFee; // RoomBuddy's commission
    
    @Column(name = "landlord_payout")
    private Integer landlordPayout; // Amount to landlord after fee
    
    // Payout to landlord
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status")
    @Builder.Default
    private PayoutStatus payoutStatus = PayoutStatus.PENDING;
    
    @Column(name = "payout_at")
    private LocalDateTime payoutAt;
    
    @Column(name = "payout_reference")
    private String payoutReference;
    
    // Unique reference for this payment
    @Column(name = "reference_code", unique = true, nullable = false)
    private String referenceCode;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum PaymentType {
        DEPOSIT,           // Security deposit
        FIRST_MONTH_RENT,  // First month's rent
        MONTHLY_RENT,      // Regular monthly rent
        AGENCY_FEE,        // Agency fees
        INITIAL_PAYMENT,   // Combined deposit + first month + agency
        REFUND             // Refund to student
    }
    
    public enum PaymentMethod {
        MTN_MOMO,          // MTN Mobile Money
        ORANGE_MONEY,      // Orange Money
        EXPRESS_UNION,     // Express Union Mobile
        BANK_TRANSFER,     // Bank transfer
        CASH               // Cash (recorded manually)
    }
    
    public enum PaymentStatus {
        PENDING,           // Waiting for payment
        SUBMITTED,         // User submitted proof, awaiting verification
        VERIFIED,          // Admin verified payment
        REJECTED,          // Payment proof rejected
        FAILED,            // Payment failed
        REFUNDED,          // Payment refunded
        CANCELLED          // Payment cancelled
    }
    
    public enum PayoutStatus {
        PENDING,           // Not yet paid to landlord
        PROCESSING,        // Payout in progress
        COMPLETED,         // Paid to landlord
        FAILED             // Payout failed
    }
    
    @PrePersist
    public void generateReferenceCode() {
        if (this.referenceCode == null) {
            // Generate: PAY-YYYYMMDD-XXXXX
            String datePart = java.time.LocalDate.now().toString().replace("-", "");
            String randomPart = String.format("%05d", (int) (Math.random() * 100000));
            this.referenceCode = "PAY-" + datePart + "-" + randomPart;
        }
    }
    
    public boolean isVerified() {
        return status == PaymentStatus.VERIFIED;
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.SUBMITTED;
    }
}
