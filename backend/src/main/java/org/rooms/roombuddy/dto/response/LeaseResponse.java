package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.Lease;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseResponse {
    
    private UUID id;
    private String referenceCode;
    
    // Listing info
    private UUID listingId;
    private String listingTitle;
    private String listingAddress;
    
    // Parties
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    
    private UUID landlordId;
    private String landlordName;
    private String landlordEmail;
    private String landlordPhone;
    
    // Co-tenant (for shared leases)
    private UUID coTenantId;
    private String coTenantName;
    private String coTenantEmail;
    private String coTenantPhone;
    private Boolean coTenantAcceptedTerms;
    private LocalDateTime coTenantAcceptedAt;
    private Boolean isSharedLease;
    
    // Terms
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer monthlyRent;
    private Integer depositAmount;
    private Integer agencyFees;
    private Integer totalInitialPayment;
    
    // Status
    private Lease.LeaseStatus status;
    private Boolean studentAcceptedTerms;
    private Boolean landlordAcceptedTerms;
    private LocalDateTime studentAcceptedAt;
    private LocalDateTime landlordAcceptedAt;
    
    // Move dates
    private LocalDate actualMoveInDate;
    private LocalDate actualMoveOutDate;
    
    // Termination
    private Lease.TerminationReason terminationReason;
    private String terminationNotes;
    private LocalDateTime terminatedAt;
    
    // Payment info
    private String paymentReferenceCode;
    private Boolean paymentVerified;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed
    private Integer durationMonths;
    private Boolean canBeReviewed;
    private Boolean isActive;
}
