package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.entity.PhoneVerificationOtp;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.PhoneVerificationOtpRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PhoneVerificationService {
    
    private final PhoneVerificationOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final WhatsAppService whatsAppService;
    
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRATION_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * Send OTP to user's phone number via WhatsApp
     */
    public void sendOtp(UUID userId, String phoneNumber) {
        log.info("Sending OTP to user {} for phone: {}", userId, phoneNumber);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Check if phone is already verified
        if (user.getPhoneVerified() != null && user.getPhoneVerified()) {
            throw new BadRequestException("Phone number is already verified");
        }

        String requestedE164 = requireCameroonE164(phoneNumber, "Phone number");
        String onFileE164 = requireCameroonE164(user.getPhone(), "Account phone");
        if (!requestedE164.equals(onFileE164)) {
            throw new BadRequestException("Phone number does not match registered phone number");
        }
        
        // Invalidate any existing unverified OTPs for this user
        Optional<PhoneVerificationOtp> existingOtp = otpRepository.findFirstByUserIdAndVerifiedFalseOrderByCreatedAtDesc(userId);
        if (existingOtp.isPresent()) {
            PhoneVerificationOtp otp = existingOtp.get();
            if (!otp.isExpired() && otp.getAttempts() < otp.getMaxAttempts()) {
                // Resend the same OTP if it's still valid
                log.info("Resending existing OTP to user {}", userId);
                whatsAppService.sendOtp(requestedE164, otp.getOtpCode(), user.getFirstName());
                return;
            }
        }
        
        // Generate new OTP
        String otpCode = generateOtp();
        
        // Create OTP record
        PhoneVerificationOtp otp = PhoneVerificationOtp.builder()
                .user(user)
                .phoneNumber(requestedE164)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES))
                .verified(false)
                .attempts(0)
                .maxAttempts(MAX_ATTEMPTS)
                .build();
        
        otpRepository.save(otp);
        
        // Send OTP via WhatsApp
        boolean sent = whatsAppService.sendOtp(requestedE164, otpCode, user.getFirstName());
        
        if (!sent) {
            log.error("Failed to send OTP via WhatsApp to user {}", userId);
            throw new BadRequestException("Failed to send OTP. Please try again later.");
        }
        
        log.info("OTP sent successfully to user {} for phone: {}", userId, requestedE164);
    }
    
    /**
     * Verify OTP code
     */
    public void verifyOtp(UUID userId, String otpCode) {
        log.info("Verifying OTP for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Find OTP
        PhoneVerificationOtp otp = otpRepository.findByUserIdAndOtpCodeAndVerifiedFalse(userId, otpCode)
                .orElseThrow(() -> new BadRequestException("Invalid OTP code"));
        
        // Check if OTP is expired
        if (otp.isExpired()) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }
        
        // Check if max attempts reached
        if (otp.isMaxAttemptsReached()) {
            throw new BadRequestException("Maximum verification attempts reached. Please request a new OTP.");
        }
        
        // Increment attempts
        otp.setAttempts(otp.getAttempts() + 1);
        
        // Verify OTP
        otp.setVerified(true);
        otp.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(otp);
        
        // Mark all OTPs for this user as verified (cleanup)
        otpRepository.markAllAsVerifiedForUser(userId, LocalDateTime.now());
        
        // Update user phone verification status
        user.setPhoneVerified(true);
        userRepository.save(user);
        
        log.info("Phone verified successfully for user: {}", userId);
    }
    
    /**
     * Resend OTP (if expired or max attempts reached)
     */
    public void resendOtp(UUID userId) {
        log.info("Resending OTP for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Invalidate existing OTPs
        Optional<PhoneVerificationOtp> existingOtp = otpRepository.findFirstByUserIdAndVerifiedFalseOrderByCreatedAtDesc(userId);
        if (existingOtp.isPresent()) {
            PhoneVerificationOtp otp = existingOtp.get();
            // Mark as verified to invalidate it
            otp.setVerified(true);
            otp.setVerifiedAt(LocalDateTime.now());
            otpRepository.save(otp);
        }
        
        // Send new OTP
        sendOtp(userId, user.getPhone());
    }
    
    /**
     * Generate 6-digit OTP
     */
    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000); // 100000 to 999999
        return String.valueOf(otp);
    }
    
    /**
     * Normalize to +237XXXXXXXXX (9 national digits) or throw.
     */
    private static String requireCameroonE164(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(label + " is missing");
        }
        String t = raw.trim().replaceAll("\\s+", "");
        if (t.matches("^\\+237[0-9]{9}$")) {
            return t;
        }
        String digits = t.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("237")) {
            return "+" + digits;
        }
        if (digits.length() == 9) {
            return "+237" + digits;
        }
        throw new BadRequestException(label + " must be a Cameroon number (+237 and 9 digits)");
    }
}

