package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.RealtorProfileRequest;
import org.rooms.roombay.dto.request.RealtorVerificationDocumentRequest;
import org.rooms.roombay.dto.response.RealtorProfileResponse;
import org.rooms.roombay.dto.response.RealtorVerificationDocumentResponse;
import org.rooms.roombay.entity.Notification;
import org.rooms.roombay.entity.RealtorProfile;
import org.rooms.roombay.entity.RealtorVerificationDocument;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.RealtorProfileRepository;
import org.rooms.roombay.repository.RealtorVerificationDocumentRepository;
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
 * Realtor onboarding + verification, mirroring the landlord-verification stack: a realtor creates a
 * profile, uploads KYC documents, and an admin approves / rejects / suspends the account.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealtorService {

    private final RealtorProfileRepository realtorProfileRepository;
    private final RealtorVerificationDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ---- Realtor-facing ----------------------------------------------------

    @Transactional
    public RealtorProfileResponse registerProfile(UUID userId, RealtorProfileRequest request) {
        if (realtorProfileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Realtor profile already exists. Use update instead.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RealtorProfile profile = RealtorProfile.builder()
                .user(user)
                .agencyName(request.getAgencyName())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .city(request.getCity())
                .areasCovered(joinAreas(request.getAreasCovered()))
                .phoneNumber(request.getPhoneNumber())
                .whatsappNumber(request.getWhatsappNumber())
                .bio(request.getBio())
                .verificationStatus(RealtorProfile.VerificationStatus.PENDING)
                .build();
        RealtorProfile saved = realtorProfileRepository.save(profile);
        log.info("Realtor profile created for user {}", userId);
        return buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public RealtorProfileResponse getMyProfile(UUID userId) {
        return buildResponse(requireProfileByUser(userId));
    }

    @Transactional
    public RealtorProfileResponse updateProfile(UUID userId, RealtorProfileRequest request) {
        RealtorProfile profile = requireProfileByUser(userId);
        profile.setAgencyName(request.getAgencyName());
        profile.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
        profile.setCity(request.getCity());
        profile.setAreasCovered(joinAreas(request.getAreasCovered()));
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setWhatsappNumber(request.getWhatsappNumber());
        profile.setBio(request.getBio());
        return buildResponse(realtorProfileRepository.save(profile));
    }

    @Transactional
    public RealtorProfileResponse addDocument(UUID userId, RealtorVerificationDocumentRequest request) {
        RealtorProfile profile = requireProfileByUser(userId);
        RealtorVerificationDocument doc = RealtorVerificationDocument.builder()
                .realtor(profile)
                .documentType(request.getDocumentType())
                .documentUrl(request.getDocumentUrl())
                .status(RealtorVerificationDocument.Status.PENDING)
                .build();
        documentRepository.save(doc);
        // A rejected realtor resubmitting documents returns to the pending queue.
        if (profile.getVerificationStatus() == RealtorProfile.VerificationStatus.REJECTED) {
            profile.setVerificationStatus(RealtorProfile.VerificationStatus.PENDING);
            profile.setRejectionReason(null);
            realtorProfileRepository.save(profile);
        }
        return buildResponse(profile);
    }

    // ---- Admin-facing ------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<RealtorProfileResponse> getByStatusForAdmin(RealtorProfile.VerificationStatus status, Pageable pageable) {
        return realtorProfileRepository.findByVerificationStatus(status, pageable).map(this::buildResponse);
    }

    @Transactional
    public RealtorProfileResponse approve(UUID realtorId, UUID adminId) {
        RealtorProfile profile = requireProfile(realtorId);
        profile.setVerificationStatus(RealtorProfile.VerificationStatus.VERIFIED);
        profile.setRejectionReason(null);
        profile.setReviewedBy(adminId);
        profile.setReviewedAt(LocalDateTime.now());
        RealtorProfile saved = realtorProfileRepository.save(profile);
        notify(saved, Notification.NotificationType.VERIFICATION_APPROVED,
                "Realtor account verified",
                "Your realtor account is now verified. You can start listing and managing properties on RoomBay.");
        return buildResponse(saved);
    }

    @Transactional
    public RealtorProfileResponse reject(UUID realtorId, UUID adminId, String reason) {
        RealtorProfile profile = requireProfile(realtorId);
        profile.setVerificationStatus(RealtorProfile.VerificationStatus.REJECTED);
        profile.setRejectionReason(reason == null || reason.isBlank() ? "Verification requirements not met." : reason);
        profile.setReviewedBy(adminId);
        profile.setReviewedAt(LocalDateTime.now());
        RealtorProfile saved = realtorProfileRepository.save(profile);
        notify(saved, Notification.NotificationType.VERIFICATION_REJECTED,
                "Realtor verification rejected", profile.getRejectionReason());
        return buildResponse(saved);
    }

    @Transactional
    public RealtorProfileResponse suspend(UUID realtorId, UUID adminId, String reason) {
        RealtorProfile profile = requireProfile(realtorId);
        profile.setVerificationStatus(RealtorProfile.VerificationStatus.SUSPENDED);
        profile.setRejectionReason(reason == null || reason.isBlank() ? "Account suspended by an administrator." : reason);
        profile.setReviewedBy(adminId);
        profile.setReviewedAt(LocalDateTime.now());
        RealtorProfile saved = realtorProfileRepository.save(profile);
        notify(saved, Notification.NotificationType.VERIFICATION_REJECTED,
                "Realtor account suspended", profile.getRejectionReason());
        return buildResponse(saved);
    }

    // ---- helpers -----------------------------------------------------------

    private RealtorProfile requireProfileByUser(UUID userId) {
        return realtorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No realtor profile found. Complete realtor onboarding first."));
    }

    private RealtorProfile requireProfile(UUID realtorId) {
        return realtorProfileRepository.findById(realtorId)
                .orElseThrow(() -> new ResourceNotFoundException("Realtor not found: " + realtorId));
    }

    private void notify(RealtorProfile profile, Notification.NotificationType type, String title, String message) {
        if (profile.getUser() == null || profile.getUser().getId() == null) {
            return;
        }
        try {
            notificationService.createNotification(profile.getUser().getId(), type, title, message,
                    profile.getId(), "REALTOR", "/realtor/dashboard");
        } catch (Exception e) {
            log.warn("Failed to send realtor notification: {}", e.getMessage());
        }
    }

    private RealtorProfileResponse buildResponse(RealtorProfile profile) {
        List<RealtorVerificationDocumentResponse> docs = documentRepository
                .findByRealtorIdOrderByCreatedAtDesc(profile.getId()).stream()
                .map(RealtorVerificationDocumentResponse::fromEntity)
                .collect(Collectors.toList());
        return RealtorProfileResponse.fromEntity(profile, docs);
    }

    private static String joinAreas(List<String> areas) {
        if (areas == null || areas.isEmpty()) {
            return null;
        }
        return areas.stream()
                .filter(a -> a != null && !a.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(", "));
    }
}
