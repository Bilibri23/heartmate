package org.rooms.roombay.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.rooms.roombay.entity.LandlordVerification;
import org.rooms.roombay.entity.StudentVerification;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect that enforces verification requirements on annotated endpoints.
 * Checks if the current user has completed required verification before allowing access.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationAspect {
    
    private final StudentVerificationRepository studentVerificationRepository;
    private final LandlordVerificationRepository landlordVerificationRepository;
    private final UserRepository userRepository;
    
    @Before("@annotation(requiresVerification)")
    public void checkVerification(RequiresVerification requiresVerification) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String userRole = SecurityUtils.getCurrentUserRole();
        
        log.debug("Checking verification for user {} with role {}", userId, userRole);
        
        // Admin users bypass verification checks
        if ("ADMIN".equals(userRole)) {
            return;
        }
        
        // Check if user role matches the required role
        if (!requiresVerification.role().equals(userRole)) {
            return; // Different role, skip check
        }
        
        // Check verification based on role
        if ("STUDENT".equals(userRole)) {
            checkStudentVerification(userId);
        } else if ("LANDLORD".equals(userRole)) {
            checkLandlordVerification(userId, requiresVerification.verificationType());
        }
    }
    
    private void checkStudentVerification(UUID userId) {
        StudentVerification verification = studentVerificationRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Student {} attempted to access protected resource without submitting verification", userId);
                    return new VerificationRequiredException("STUDENT", "Student ID verification");
                });
        
        if (verification.getStatus() != StudentVerification.Status.VERIFIED) {
            log.warn("Student {} attempted to access protected resource with verification status: {}", 
                    userId, verification.getStatus());
            throw new VerificationRequiredException("STUDENT", 
                    "Student ID verification (current status: " + verification.getStatus() + ")");
        }
    }
    
    private void checkLandlordVerification(UUID userId, String verificationType) {
        LandlordVerification verification = landlordVerificationRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Landlord {} attempted to access protected resource without submitting verification", userId);
                    return new VerificationRequiredException("LANDLORD", verificationType + " verification");
                });
        
        // Check specific verification type
        boolean isVerified = switch (verificationType) {
            case "IDENTITY" -> verification.getIdentityStatus() == LandlordVerification.VerificationStatus.VERIFIED;
            case "BUSINESS" -> verification.getBusinessStatus() == LandlordVerification.VerificationStatus.VERIFIED;
            case "PROPERTY" -> verification.getPropertyStatus() == LandlordVerification.VerificationStatus.VERIFIED;
            default -> false;
        };
        
        if (!isVerified) {
            log.warn("Landlord {} attempted to access protected resource without {} verification", 
                    userId, verificationType);
            throw new VerificationRequiredException("LANDLORD", verificationType + " verification");
        }
    }
}
