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
 * Agency/agent profile for a user whose role is {@link User.UserRole#REALTOR}. A realtor helps
 * landlords list and manage properties, and must be verified by an admin before being trusted.
 */
@Entity
@Table(name = "realtor_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "agency_name")
    private String agencyName;

    @Column(name = "business_registration_number")
    private String businessRegistrationNumber;

    private String city;

    /** Comma-separated list of neighborhoods/areas the realtor covers. */
    @Column(name = "areas_covered", columnDefinition = "text")
    private String areasCovered;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(columnDefinition = "text")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "trust_score", nullable = false)
    @Builder.Default
    private Integer trustScore = 0;

    @Column(name = "total_listings", nullable = false)
    @Builder.Default
    private Integer totalListings = 0;

    @Column(name = "successful_rentals", nullable = false)
    @Builder.Default
    private Integer successfulRentals = 0;

    @Column(name = "complaint_count", nullable = false)
    @Builder.Default
    private Integer complaintCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED, SUSPENDED
    }
}
