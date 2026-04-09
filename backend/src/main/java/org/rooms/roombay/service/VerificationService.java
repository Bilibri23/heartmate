package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.VerificationApprovalRequest;
import org.rooms.roombay.dto.request.VerificationRequest;
import org.rooms.roombay.dto.response.VerificationResponse;
import org.rooms.roombay.entity.StudentVerification;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VerificationService {
    
    private final StudentVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    
    public VerificationResponse submitVerification(UUID userId, VerificationRequest request) {
        log.info("Submitting verification request for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        if (user.getRole() != User.UserRole.STUDENT) {
            throw new BadRequestException("Only tenants can submit verification requests");
        }
        
        boolean hasGovId = request.getIdType() != null && !request.getIdType().isBlank()
                && request.getIdNumber() != null && !request.getIdNumber().isBlank()
                && request.getIdPhotoUrl() != null && !request.getIdPhotoUrl().isBlank();
        boolean hasStudentId = request.getUniversity() != null && !request.getUniversity().isBlank()
                && request.getStudentId() != null && !request.getStudentId().isBlank()
                && request.getStudentIdPhotoUrl() != null && !request.getStudentIdPhotoUrl().isBlank();
        
        if (!hasGovId && !hasStudentId) {
            throw new BadRequestException("Provide either government ID (idType, idNumber, idPhotoUrl) or legacy campus ID (university, studentId, studentIdPhotoUrl)");
        }
        
        if (verificationRepository.existsByUserId(userId)) {
            StudentVerification existing = verificationRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Verification not found"));
            
            if (existing.getStatus() == StudentVerification.Status.REJECTED) {
                applyRequestToVerification(existing, request);
                existing.setStatus(StudentVerification.Status.PENDING);
                existing.setRejectionReason(null);
                existing.setVerifiedBy(null);
                existing.setVerifiedAt(null);
                
                StudentVerification updated = verificationRepository.save(existing);
                log.info("Verification resubmitted for user: {}", userId);
                return mapToResponse(updated);
            } else {
                throw new BadRequestException("Verification request already exists with status: " + existing.getStatus());
            }
        }
        
        StudentVerification verification = StudentVerification.builder()
                .user(user)
                .status(StudentVerification.Status.PENDING)
                .build();
        applyRequestToVerification(verification, request);
        
        StudentVerification saved = verificationRepository.save(verification);
        log.info("Verification request submitted successfully for user: {}", userId);
        
        return mapToResponse(saved);
    }
    
    private void applyRequestToVerification(StudentVerification v, VerificationRequest r) {
        v.setUniversity(r.getUniversity());
        v.setStudentId(r.getStudentId());
        v.setFaculty(r.getFaculty());
        v.setDepartment(r.getDepartment());
        v.setYearOfStudy(r.getYearOfStudy());
        v.setStudentIdPhotoUrl(r.getStudentIdPhotoUrl());
        v.setIdType(r.getIdType());
        v.setIdNumber(r.getIdNumber());
        v.setIdPhotoUrl(r.getIdPhotoUrl());
        v.setSelfiePhotoUrl(r.getSelfiePhotoUrl());
    }
    
    @Transactional(readOnly = true)
    public VerificationResponse getVerification(UUID userId) {
        log.info("Fetching verification for user: {}", userId);
        
        StudentVerification verification = verificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found for user: " + userId));
        
        return mapToResponse(verification);
    }
    
    @Transactional(readOnly = true)
    public VerificationResponse getVerificationById(UUID verificationId) {
        log.info("Fetching verification by id: {}", verificationId);
        
        StudentVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found with id: " + verificationId));
        
        return mapToResponse(verification);
    }
    
    public VerificationResponse updateVerification(UUID userId, VerificationRequest request) {
        log.info("Updating verification for user: {}", userId);
        
        StudentVerification verification = verificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found for user: " + userId));
        
        // Only allow updates if status is PENDING or REJECTED
        if (verification.getStatus() == StudentVerification.Status.VERIFIED) {
            throw new BadRequestException("Cannot update verified verification");
        }
        
        // Update fields
        if (request.getUniversity() != null) {
            verification.setUniversity(request.getUniversity());
        }
        if (request.getStudentId() != null) {
            verification.setStudentId(request.getStudentId());
        }
        if (request.getFaculty() != null) {
            verification.setFaculty(request.getFaculty());
        }
        if (request.getDepartment() != null) {
            verification.setDepartment(request.getDepartment());
        }
        if (request.getYearOfStudy() != null) {
            verification.setYearOfStudy(request.getYearOfStudy());
        }
        if (request.getStudentIdPhotoUrl() != null) {
            verification.setStudentIdPhotoUrl(request.getStudentIdPhotoUrl());
        }
        if (request.getIdType() != null) verification.setIdType(request.getIdType());
        if (request.getIdNumber() != null) verification.setIdNumber(request.getIdNumber());
        if (request.getIdPhotoUrl() != null) verification.setIdPhotoUrl(request.getIdPhotoUrl());
        if (request.getSelfiePhotoUrl() != null) verification.setSelfiePhotoUrl(request.getSelfiePhotoUrl());
        
        // Reset status to PENDING if it was REJECTED
        if (verification.getStatus() == StudentVerification.Status.REJECTED) {
            verification.setStatus(StudentVerification.Status.PENDING);
            verification.setRejectionReason(null);
            verification.setVerifiedBy(null);
            verification.setVerifiedAt(null);
        }
        
        StudentVerification updated = verificationRepository.save(verification);
        log.info("Verification updated successfully for user: {}", userId);
        
        return mapToResponse(updated);
    }
    
    public void deleteVerification(UUID userId) {
        log.info("Deleting verification for user: {}", userId);
        
        if (!verificationRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("Verification not found for user: " + userId);
        }
        
        verificationRepository.deleteByUserId(userId);
        log.info("Verification deleted successfully for user: {}", userId);
    }
    
    public VerificationResponse approveOrRejectVerification(UUID verificationId, UUID adminId, VerificationApprovalRequest request) {
        log.info("Admin {} approving/rejecting verification: {}", adminId, verificationId);
        
        StudentVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found with id: " + verificationId));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));
        
        // Check if admin has ADMIN role
        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new BadRequestException("Only admins can approve/reject verifications");
        }
        
        // Validate status
        StudentVerification.Status status;
        try {
            status = StudentVerification.Status.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + request.getStatus() + ". Must be VERIFIED or REJECTED");
        }
        
        if (status != StudentVerification.Status.VERIFIED && status != StudentVerification.Status.REJECTED) {
            throw new BadRequestException("Status must be VERIFIED or REJECTED");
        }
        
        // Validate rejection reason
        if (status == StudentVerification.Status.REJECTED && 
            (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty())) {
            throw new BadRequestException("Rejection reason is required when rejecting verification");
        }
        
        // Update verification
        verification.setStatus(status);
        verification.setRejectionReason(status == StudentVerification.Status.REJECTED ? request.getRejectionReason() : null);
        verification.setVerifiedBy(admin);
        verification.setVerifiedAt(LocalDateTime.now());
        
        StudentVerification updated = verificationRepository.save(verification);
        
        // Send email notification
        emailService.sendVerificationStatusEmail(
                verification.getUser().getEmail(),
                status.name(),
                request.getRejectionReason()
        );
        
        // Send in-app notification
        if (status == StudentVerification.Status.VERIFIED) {
            notificationService.createNotification(
                    verification.getUser().getId(),
                    org.rooms.roombay.entity.Notification.NotificationType.VERIFICATION_APPROVED,
                    "Verification Approved!",
                    "Your student verification has been approved. You can now access all features.",
                    verificationId,
                    "VERIFICATION",
                    "/admin/student/verification"
            );
        } else {
            notificationService.createNotification(
                    verification.getUser().getId(),
                    org.rooms.roombay.entity.Notification.NotificationType.VERIFICATION_REJECTED,
                    "Verification Rejected",
                    "Your verification was rejected: " + request.getRejectionReason(),
                    verificationId,
                    "VERIFICATION",
                    "/admin/student/verification"
            );
        }
        
        log.info("Verification {} {} by admin {}", verificationId, status, adminId);
        
        return mapToResponse(updated);
    }
    
    @Transactional(readOnly = true)
    public List<VerificationResponse> getAllVerifications(StudentVerification.Status status) {
        log.info("Fetching all verifications with status: {}", status);
        
        List<StudentVerification> verifications;
        if (status != null) {
            verifications = verificationRepository.findAll().stream()
                    .filter(v -> v.getStatus() == status)
                    .collect(Collectors.toList());
        } else {
            verifications = verificationRepository.findAll();
        }
        
        return verifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<VerificationResponse> getPendingVerifications() {
        log.info("Fetching pending verifications");
        return getAllVerifications(StudentVerification.Status.PENDING);
    }
    
    private VerificationResponse mapToResponse(StudentVerification verification) {
        User u = verification.getUser();
        return VerificationResponse.builder()
                .id(verification.getId())
                .userId(u.getId())
                .userName(u.getFirstName() + " " + u.getLastName())
                .userEmail(u.getEmail())
                .university(verification.getUniversity())
                .studentId(verification.getStudentId())
                .faculty(verification.getFaculty())
                .department(verification.getDepartment())
                .yearOfStudy(verification.getYearOfStudy())
                .studentIdPhotoUrl(verification.getStudentIdPhotoUrl())
                .idType(verification.getIdType())
                .idNumber(verification.getIdNumber())
                .idPhotoUrl(verification.getIdPhotoUrl())
                .selfiePhotoUrl(verification.getSelfiePhotoUrl())
                .status(verification.getStatus() != null ? verification.getStatus().name() : null)
                .rejectionReason(verification.getRejectionReason())
                .verifiedBy(verification.getVerifiedBy() != null ? verification.getVerifiedBy().getId() : null)
                .verifiedAt(verification.getVerifiedAt())
                .createdAt(verification.getCreatedAt())
                .updatedAt(verification.getUpdatedAt())
                .build();
    }
}

