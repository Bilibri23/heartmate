package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.SendOtpRequest;
import org.rooms.roombuddy.dto.request.VerifyOtpRequest;
import org.rooms.roombuddy.dto.response.ApiResponse;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.PhoneVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/phone-verification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Phone Verification", description = "APIs for phone/WhatsApp verification via OTP")
@SecurityRequirement(name = "bearerAuth")
public class PhoneVerificationController {
    
    private final PhoneVerificationService phoneVerificationService;
    
    @PostMapping("/send")
    @Operation(summary = "Send OTP", description = "Send OTP code to user's phone number via WhatsApp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @RequestParam(required = false) UUID ignoredUserId,
            @Valid @RequestBody SendOtpRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Sending OTP to user: {} for phone: {}", userId, request.getPhoneNumber());
        
        phoneVerificationService.sendOtp(userId, request.getPhoneNumber());
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("OTP sent successfully to your WhatsApp number")
                .build());
    }
    
    @PostMapping("/verify")
    @Operation(summary = "Verify OTP", description = "Verify OTP code to confirm phone number")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @RequestParam(required = false) UUID ignoredUserId,
            @Valid @RequestBody VerifyOtpRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Verifying OTP for user: {}", userId);
        
        phoneVerificationService.verifyOtp(userId, request.getOtpCode());
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Phone number verified successfully")
                .build());
    }
    
    @PostMapping("/resend")
    @Operation(summary = "Resend OTP", description = "Resend OTP code if expired or max attempts reached")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestParam(required = false) UUID ignoredUserId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Resending OTP for user: {}", userId);
        
        phoneVerificationService.resendOtp(userId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("OTP resent successfully to your WhatsApp number")
                .build());
    }
}

