package org.rooms.roombay.entity;

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
 * LandlordVerification entity for KYC (Know Your Customer) verification.
 * This is critical for scam prevention in the housing marketplace.
 * 
 * Verification levels:
 * - BASIC: Email + Phone verified
 * - IDENTITY: Government ID verified
 * - BUSINESS: Business registration verified (for property managers)
 * - PROPERTY: Property ownership documents verified
 */
@Entity
@Table(name = "landlord_verification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandlordVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ==================== IDENTITY VERIFICATION ====================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "id_type")
    private IdDocumentType idType;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "id_front_photo_url")
    private String idFrontPhotoUrl;

    @Column(name = "id_back_photo_url")
    private String idBackPhotoUrl;

    @Column(name = "selfie_with_id_url")
    private String selfieWithIdUrl;

    @Column(name = "id_expiry_date")
    private String idExpiryDate;

    // ==================== BUSINESS VERIFICATION (Optional) ====================
    
    @Column(name = "business_name")
    private String businessName;

    @Column(name = "business_registration_number")
    private String businessRegistrationNumber;

    @Column(name = "business_registration_doc_url")
    private String businessRegistrationDocUrl;

    @Column(name = "tax_id_number")
    private String taxIdNumber;

    // ==================== PROPERTY OWNERSHIP VERIFICATION ====================
    
    @Column(name = "property_ownership_doc_url")
    private String propertyOwnershipDocUrl;

    @Column(name = "utility_bill_url")
    private String utilityBillUrl;

    // ==================== ADDRESS VERIFICATION ====================
    
    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "region")
    private String region;

    @Column(name = "postal_code")
    private String postalCode;

    // ==================== VERIFICATION STATUS ====================
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "verification_level")
    private VerificationLevel verificationLevel = VerificationLevel.NONE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "identity_status")
    private VerificationStatus identityStatus = VerificationStatus.NOT_SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "business_status")
    private VerificationStatus businessStatus = VerificationStatus.NOT_SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "property_status")
    private VerificationStatus propertyStatus = VerificationStatus.NOT_SUBMITTED;

    @Column(name = "identity_rejection_reason", columnDefinition = "TEXT")
    private String identityRejectionReason;

    @Column(name = "business_rejection_reason", columnDefinition = "TEXT")
    private String businessRejectionReason;

    @Column(name = "property_rejection_reason", columnDefinition = "TEXT")
    private String propertyRejectionReason;

    // ==================== ADMIN REVIEW ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // ==================== TRUST SCORE ====================
    
    @Builder.Default
    @Column(name = "trust_score")
    private Integer trustScore = 0;

    @Builder.Default
    @Column(name = "is_trusted_landlord")
    private Boolean isTrustedLandlord = false;

    @Builder.Default
    @Column(name = "total_listings")
    private Integer totalListings = 0;

    @Builder.Default
    @Column(name = "successful_rentals")
    private Integer successfulRentals = 0;

    @Builder.Default
    @Column(name = "reported_count")
    private Integer reportedCount = 0;

    // ==================== TIMESTAMPS ====================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== ENUMS ====================
    
    public enum IdDocumentType {
        NATIONAL_ID,           // Cameroon National ID Card (CNI)
        PASSPORT,              // International Passport
        DRIVERS_LICENSE,       // Driver's License
        VOTER_CARD,            // Voter's Card
        RESIDENCE_PERMIT       // For foreign nationals
    }

    public enum VerificationLevel {
        NONE,                  // No verification
        BASIC,                 // Email + Phone verified
        IDENTITY,              // Government ID verified
        BUSINESS,              // Business registration verified
        PROPERTY,              // Property ownership verified
        FULLY_VERIFIED         // All verifications complete
    }

    public enum VerificationStatus {
        NOT_SUBMITTED,
        PENDING,
        VERIFIED,
        REJECTED,
        EXPIRED
    }

    // ==================== HELPER METHODS ====================
    
    /**
     * Calculate trust score based on verification status and history
     */
    public void calculateTrustScore() {
        int score = 0;
        
        // Base score for identity verification
        if (identityStatus == VerificationStatus.VERIFIED) {
            score += 30;
        }
        
        // Business verification bonus
        if (businessStatus == VerificationStatus.VERIFIED) {
            score += 20;
        }
        
        // Property verification bonus
        if (propertyStatus == VerificationStatus.VERIFIED) {
            score += 20;
        }
        
        // Successful rentals bonus (max 20 points)
        score += Math.min(successfulRentals * 2, 20);
        
        // Penalty for reports
        score -= reportedCount * 5;
        
        // Ensure score is between 0 and 100
        this.trustScore = Math.max(0, Math.min(100, score));
        
        // Mark as trusted if score >= 70
        this.isTrustedLandlord = this.trustScore >= 70;
    }

    /**
     * Update verification level based on current status
     */
    public void updateVerificationLevel() {
        if (identityStatus == VerificationStatus.VERIFIED &&
            businessStatus == VerificationStatus.VERIFIED &&
            propertyStatus == VerificationStatus.VERIFIED) {
            this.verificationLevel = VerificationLevel.FULLY_VERIFIED;
        } else if (propertyStatus == VerificationStatus.VERIFIED) {
            this.verificationLevel = VerificationLevel.PROPERTY;
        } else if (businessStatus == VerificationStatus.VERIFIED) {
            this.verificationLevel = VerificationLevel.BUSINESS;
        } else if (identityStatus == VerificationStatus.VERIFIED) {
            this.verificationLevel = VerificationLevel.IDENTITY;
        } else {
            this.verificationLevel = VerificationLevel.BASIC;
        }
        
        calculateTrustScore();
    }
}
