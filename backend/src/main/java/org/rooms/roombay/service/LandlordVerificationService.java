package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.BusinessVerificationRequest;
import org.rooms.roombay.dto.request.LandlordVerificationApprovalRequest;
import org.rooms.roombay.dto.request.LandlordVerificationRequest;
import org.rooms.roombay.dto.request.PropertyVerificationRequest;
import org.rooms.roombay.dto.response.LandlordVerificationResponse;
import org.rooms.roombay.entity.LandlordVerification;
import org.rooms.roombay.entity.LandlordVerification.IdDocumentType;
import org.rooms.roombay.entity.LandlordVerification.VerificationStatus;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing landlord KYC verification.
 * Handles identity, business, and property verification workflows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LandlordVerificationService {
    
    private final LandlordVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    
    // ==================== IDENTITY VERIFICATION ====================
    
    /**
     * Submit identity verification (KYC) for a landlord.
     * This is the first and most important step.
     */
    public LandlordVerificationResponse submitIdentityVerification(UUID userId, LandlordVerificationRequest request) {
        log.info("Submitting identity verification for landlord: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Verify user is a landlord
        if (user.getRole() != User.UserRole.LANDLORD) {
            throw new BadRequestException("Only landlords can submit verification requests");
        }
        
        // Parse ID type
        IdDocumentType idType;
        try {
            idType = IdDocumentType.valueOf(request.getIdType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid ID type: " + request.getIdType());
        }
        
        LandlordVerification verification = verificationRepository.findByUserId(userId)
                .orElse(null);
        
        if (verification == null) {
            // Create new verification record
            verification = LandlordVerification.builder()
                    .user(user)
                    .idType(idType)
                    .idNumber(request.getIdNumber())
                    .idFrontPhotoUrl(request.getIdFrontPhotoUrl())
                    .idBackPhotoUrl(request.getIdBackPhotoUrl())
                    .selfieWithIdUrl(request.getSelfieWithIdUrl())
                    .idExpiryDate(request.getIdExpiryDate())
                    .addressLine1(request.getAddressLine1())
                    .addressLine2(request.getAddressLine2())
                    .city(request.getCity())
                    .region(request.getRegion())
                    .postalCode(request.getPostalCode())
                    .identityStatus(VerificationStatus.PENDING)
                    .build();
        } else {
            // Update existing verification
            if (verification.getIdentityStatus() == VerificationStatus.VERIFIED) {
                throw new BadRequestException("Identity already verified. Contact support to update.");
            }
            
            verification.setIdType(idType);
            verification.setIdNumber(request.getIdNumber());
            verification.setIdFrontPhotoUrl(request.getIdFrontPhotoUrl());
            verification.setIdBackPhotoUrl(request.getIdBackPhotoUrl());
            verification.setSelfieWithIdUrl(request.getSelfieWithIdUrl());
            verification.setIdExpiryDate(request.getIdExpiryDate());
            verification.setAddressLine1(request.getAddressLine1());
            verification.setAddressLine2(request.getAddressLine2());
            verification.setCity(request.getCity());
            verification.setRegion(request.getRegion());
            verification.setPostalCode(request.getPostalCode());
            verification.setIdentityStatus(VerificationStatus.PENDING);
            verification.setIdentityRejectionReason(null);
        }
        
        LandlordVerification saved = verificationRepository.save(verification);
        log.info("Identity verification submitted for landlord: {}", userId);
        
        return mapToResponse(saved);
    }
    
    // ==================== BUSINESS VERIFICATION ====================
    
    /**
     * Submit business verification for a landlord (optional, for property managers).
     */
    public LandlordVerificationResponse submitBusinessVerification(UUID userId, BusinessVerificationRequest request) {
        log.info("Submitting business verification for landlord: {}", userId);
        
        LandlordVerification verification = verificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Please submit identity verification first"));
        
        // Require identity verification first
        if (verification.getIdentityStatus() != VerificationStatus.VERIFIED) {
            throw new BadRequestException("Identity must be verified before business verification");
        }
        
        if (verification.getBusinessStatus() == VerificationStatus.VERIFIED) {
            throw new BadRequestException("Business already verified. Contact support to update.");
        }
        
        verification.setBusinessName(request.getBusinessName());
        verification.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
        verification.setBusinessRegistrationDocUrl(request.getBusinessRegistrationDocUrl());
        verification.setTaxIdNumber(request.getTaxIdNumber());
        verification.setBusinessStatus(VerificationStatus.PENDING);
        verification.setBusinessRejectionReason(null);
        
        LandlordVerification saved = verificationRepository.save(verification);
        log.info("Business verification submitted for landlord: {}", userId);
        
        return mapToResponse(saved);
    }
    
    // ==================== PROPERTY VERIFICATION ====================
    
    /**
     * Submit property ownership verification for a landlord.
     */
    public LandlordVerificationResponse submitPropertyVerification(UUID userId, PropertyVerificationRequest request) {
        log.info("Submitting property verification for landlord: {}", userId);
        
        LandlordVerification verification = verificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Please submit identity verification first"));
        
        // Allow property docs alongside a pending identity packet (admin reviews together)
        VerificationStatus idStatus = verification.getIdentityStatus();
        if (idStatus != VerificationStatus.VERIFIED && idStatus != VerificationStatus.PENDING) {
            throw new BadRequestException("Submit identity verification before uploading property documents");
        }
        
        if (verification.getPropertyStatus() == VerificationStatus.VERIFIED) {
            throw new BadRequestException("Property already verified. Contact support to update.");
        }
        
        verification.setPropertyOwnershipDocUrl(request.getPropertyOwnershipDocUrl());
        verification.setUtilityBillUrl(request.getUtilityBillUrl());
        verification.setPropertyStatus(VerificationStatus.PENDING);
        verification.setPropertyRejectionReason(null);
        
        LandlordVerification saved = verificationRepository.save(verification);
        log.info("Property verification submitted for landlord: {}", userId);
        
        return mapToResponse(saved);
    }
    
    // ==================== ADMIN APPROVAL ====================
    
    /**
     * Admin approves or rejects a specific verification type.
     */
    public LandlordVerificationResponse approveOrRejectVerification(
            UUID verificationId, 
            UUID adminId, 
            LandlordVerificationApprovalRequest request) {
        
        log.info("Admin {} processing verification {} - type: {}, status: {}", 
                adminId, verificationId, request.getVerificationType(), request.getStatus());
        
        LandlordVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found: " + verificationId));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + adminId));
        
        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new BadRequestException("Only admins can approve/reject verifications");
        }
        
        // Parse status
        VerificationStatus status;
        try {
            status = VerificationStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + request.getStatus());
        }
        
        if (status != VerificationStatus.VERIFIED && status != VerificationStatus.REJECTED) {
            throw new BadRequestException("Status must be VERIFIED or REJECTED");
        }
        
        // Validate rejection reason
        if (status == VerificationStatus.REJECTED && 
            (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty())) {
            throw new BadRequestException("Rejection reason is required when rejecting");
        }
        
        String verificationType = request.getVerificationType().toUpperCase();
        String previousStatus = null;
        
        switch (verificationType) {
            case "IDENTITY":
                previousStatus = verification.getIdentityStatus().name();
                verification.setIdentityStatus(status);
                verification.setIdentityRejectionReason(
                        status == VerificationStatus.REJECTED ? request.getRejectionReason() : null);
                break;
            case "BUSINESS":
                previousStatus = verification.getBusinessStatus().name();
                verification.setBusinessStatus(status);
                verification.setBusinessRejectionReason(
                        status == VerificationStatus.REJECTED ? request.getRejectionReason() : null);
                break;
            case "PROPERTY":
                previousStatus = verification.getPropertyStatus().name();
                verification.setPropertyStatus(status);
                verification.setPropertyRejectionReason(
                        status == VerificationStatus.REJECTED ? request.getRejectionReason() : null);
                break;
            default:
                throw new BadRequestException("Invalid verification type: " + verificationType);
        }
        
        verification.setReviewedBy(admin);
        verification.setReviewedAt(LocalDateTime.now());
        if (request.getAdminNotes() != null) {
            verification.setAdminNotes(request.getAdminNotes());
        }
        
        // Update verification level and trust score
        verification.updateVerificationLevel();
        
        LandlordVerification saved = verificationRepository.save(verification);
        
        // Log audit trail
        auditLogService.logAdminAction(
                adminId,
                "LANDLORD_VERIFICATION_" + status.name(),
                "LandlordVerification",
                verificationId,
                String.format("%s verification: %s -> %s. Reason: %s", 
                        verificationType, previousStatus, status.name(), 
                        request.getRejectionReason() != null ? request.getRejectionReason() : "N/A")
        );
        
        // Send email notification to landlord
        sendVerificationStatusEmail(verification.getUser(), verificationType, status, request.getRejectionReason());
        
        log.info("Verification {} {} by admin {}", verificationId, status, adminId);
        
        return mapToResponse(saved);
    }
    
    // ==================== QUERY METHODS ====================
    
    @Transactional(readOnly = true)
    public LandlordVerificationResponse getVerification(UUID userId) {
        return verificationRepository.findByUserId(userId)
                .map(this::mapToResponse)
                .orElseGet(() -> buildNotStartedVerificationResponse(userId));
    }

    /**
     * When a landlord has never started KYC, there is no row yet — return a stable "not started" payload instead of 404.
     */
    private LandlordVerificationResponse buildNotStartedVerificationResponse(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        String displayName = formatUserDisplayName(user);
        return LandlordVerificationResponse.builder()
                .id(null)
                .userId(user.getId())
                .userName(displayName)
                .userEmail(user.getEmail())
                .userPhone(user.getPhone())
                .identityStatus(VerificationStatus.NOT_SUBMITTED.name())
                .businessStatus(VerificationStatus.NOT_SUBMITTED.name())
                .propertyStatus(VerificationStatus.NOT_SUBMITTED.name())
                .verificationLevel(LandlordVerification.VerificationLevel.NONE.name())
                .trustScore(0)
                .isTrustedLandlord(false)
                .totalListings(0)
                .successfulRentals(0)
                .reportedCount(0)
                .build();
    }

    private static String formatUserDisplayName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? "Landlord" : combined;
    }
    
    @Transactional(readOnly = true)
    public LandlordVerificationResponse getVerificationById(UUID verificationId) {
        LandlordVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found: " + verificationId));
        return mapToResponse(verification);
    }
    
    @Transactional(readOnly = true)
    public List<LandlordVerificationResponse> getPendingVerifications() {
        return verificationRepository.findAllPendingVerifications().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<LandlordVerificationResponse> getPendingVerifications(Pageable pageable) {
        return verificationRepository.findAllPendingVerifications(pageable)
                .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public List<LandlordVerificationResponse> getVerificationsByIdentityStatus(VerificationStatus status) {
        return verificationRepository.findByIdentityStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<LandlordVerificationResponse> getTrustedLandlords() {
        return verificationRepository.findByIsTrustedLandlordTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<LandlordVerificationResponse> getReportedLandlords() {
        return verificationRepository.findReportedLandlords().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    // ==================== TRUST SCORE MANAGEMENT ====================
    
    /**
     * Increment successful rentals count and recalculate trust score.
     */
    public void recordSuccessfulRental(UUID userId) {
        LandlordVerification verification = verificationRepository.findByUserId(userId)
                .orElse(null);
        
        if (verification != null) {
            verification.setSuccessfulRentals(verification.getSuccessfulRentals() + 1);
            verification.calculateTrustScore();
            verificationRepository.save(verification);
            log.info("Recorded successful rental for landlord: {}. New trust score: {}", 
                    userId, verification.getTrustScore());
        }
    }
    
    /**
     * Increment report count and recalculate trust score.
     */
    public void recordReport(UUID userId) {
        LandlordVerification verification = verificationRepository.findByUserId(userId)
                .orElse(null);
        
        if (verification != null) {
            verification.setReportedCount(verification.getReportedCount() + 1);
            verification.calculateTrustScore();
            verificationRepository.save(verification);
            log.info("Recorded report for landlord: {}. New trust score: {}", 
                    userId, verification.getTrustScore());
        }
    }
    
    /**
     * Update total listings count.
     */
    public void updateListingsCount(UUID userId, int count) {
        LandlordVerification verification = verificationRepository.findByUserId(userId)
                .orElse(null);
        
        if (verification != null) {
            verification.setTotalListings(count);
            verificationRepository.save(verification);
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private void sendVerificationStatusEmail(User user, String verificationType, 
            VerificationStatus status, String rejectionReason) {
        try {
            String subject = status == VerificationStatus.VERIFIED 
                    ? "Your " + verificationType.toLowerCase() + " verification is approved!"
                    : "Your " + verificationType.toLowerCase() + " verification needs attention";
            
            String message = status == VerificationStatus.VERIFIED
                    ? "Congratulations! Your " + verificationType.toLowerCase() + 
                      " has been verified. You can now list properties with a verified badge."
                    : "Your " + verificationType.toLowerCase() + 
                      " verification was not approved. Reason: " + rejectionReason + 
                      ". Please resubmit with the correct documents.";
            
            emailService.sendSimpleEmail(user.getEmail(), subject, message);
        } catch (Exception e) {
            log.error("Failed to send verification status email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
    
    private LandlordVerificationResponse mapToResponse(LandlordVerification v) {
        return LandlordVerificationResponse.builder()
                .id(v.getId())
                .userId(v.getUser().getId())
                .userName(v.getUser().getFirstName() + " " + v.getUser().getLastName())
                .userEmail(v.getUser().getEmail())
                .userPhone(v.getUser().getPhone())
                // Identity
                .idType(v.getIdType() != null ? v.getIdType().name() : null)
                .idNumber(v.getIdNumber())
                .idFrontPhotoUrl(v.getIdFrontPhotoUrl())
                .idBackPhotoUrl(v.getIdBackPhotoUrl())
                .selfieWithIdUrl(v.getSelfieWithIdUrl())
                .idExpiryDate(v.getIdExpiryDate())
                .identityStatus(v.getIdentityStatus() != null ? v.getIdentityStatus().name() : null)
                .identityRejectionReason(v.getIdentityRejectionReason())
                // Business
                .businessName(v.getBusinessName())
                .businessRegistrationNumber(v.getBusinessRegistrationNumber())
                .businessRegistrationDocUrl(v.getBusinessRegistrationDocUrl())
                .taxIdNumber(v.getTaxIdNumber())
                .businessStatus(v.getBusinessStatus() != null ? v.getBusinessStatus().name() : null)
                .businessRejectionReason(v.getBusinessRejectionReason())
                // Property
                .propertyOwnershipDocUrl(v.getPropertyOwnershipDocUrl())
                .utilityBillUrl(v.getUtilityBillUrl())
                .propertyStatus(v.getPropertyStatus() != null ? v.getPropertyStatus().name() : null)
                .propertyRejectionReason(v.getPropertyRejectionReason())
                // Address
                .addressLine1(v.getAddressLine1())
                .addressLine2(v.getAddressLine2())
                .city(v.getCity())
                .region(v.getRegion())
                .postalCode(v.getPostalCode())
                // Overall
                .verificationLevel(v.getVerificationLevel() != null ? v.getVerificationLevel().name() : null)
                .trustScore(v.getTrustScore())
                .isTrustedLandlord(v.getIsTrustedLandlord())
                // Stats
                .totalListings(v.getTotalListings())
                .successfulRentals(v.getSuccessfulRentals())
                .reportedCount(v.getReportedCount())
                // Admin
                .reviewedBy(v.getReviewedBy() != null ? v.getReviewedBy().getId() : null)
                .reviewedAt(v.getReviewedAt())
                .adminNotes(v.getAdminNotes())
                // Timestamps
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
