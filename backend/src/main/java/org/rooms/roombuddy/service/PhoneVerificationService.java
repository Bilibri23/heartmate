package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.entity.PhoneVerificationOtp;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.PhoneVerificationOtpRepository;
import org.rooms.roombuddy.repository.UserRepository;
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
        
        // Validate phone number format
        if (!isValidCameroonPhoneNumber(phoneNumber)) {
            throw new BadRequestException("Invalid phone number format. Must be +237XXXXXXXXX");
        }
        
        // Check if phone is already verified
        if (user.getPhoneVerified() != null && user.getPhoneVerified()) {
            throw new BadRequestException("Phone number is already verified");
        }
        
        // Check if phone number matches user's registered phone
        if (!user.getPhone().equals(phoneNumber)) {
            throw new BadRequestException("Phone number does not match registered phone number");
        }
        
        // Invalidate any existing unverified OTPs for this user
        Optional<PhoneVerificationOtp> existingOtp = otpRepository.findFirstByUserIdAndVerifiedFalseOrderByCreatedAtDesc(userId);
        if (existingOtp.isPresent()) {
            PhoneVerificationOtp otp = existingOtp.get();
            if (!otp.isExpired() && otp.getAttempts() < otp.getMaxAttempts()) {
                // Resend the same OTP if it's still valid
                log.info("Resending existing OTP to user {}", userId);
                whatsAppService.sendOtp(phoneNumber, otp.getOtpCode(), user.getFirstName());
                return;
            }
        }
        
        // Generate new OTP
        String otpCode = generateOtp();
        
        // Create OTP record
        PhoneVerificationOtp otp = PhoneVerificationOtp.builder()
                .user(user)
                .phoneNumber(phoneNumber)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES))
                .verified(false)
                .attempts(0)
                .maxAttempts(MAX_ATTEMPTS)
                .build();
        
        otpRepository.save(otp);
        
        // Send OTP via WhatsApp
        boolean sent = whatsAppService.sendOtp(phoneNumber, otpCode, user.getFirstName());
        
        if (!sent) {
            log.error("Failed to send OTP via WhatsApp to user {}", userId);
            throw new BadRequestException("Failed to send OTP. Please try again later.");
        }
        
        log.info("OTP sent successfully to user {} for phone: {}", userId, phoneNumber);
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
     * Validate Cameroon phone number format
     */
    private boolean isValidCameroonPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        // Format: +237XXXXXXXXX (9 digits after +237)
        return phoneNumber.matches("^\\+237[0-9]{9}$");
    }
}

