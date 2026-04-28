package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.LoginRequest;
import org.rooms.roombay.dto.request.PasswordResetRequest;
import org.rooms.roombay.dto.request.RegisterRequest;
import org.rooms.roombay.dto.request.ResetPasswordRequest;
import org.rooms.roombay.dto.request.ChangePasswordRequest;
import org.rooms.roombay.dto.request.UpdateMeRequest;
import org.rooms.roombay.dto.response.AuthMeResponse;
import org.rooms.roombay.dto.response.AuthResponse;
import org.rooms.roombay.dto.response.ApiResponse;
import org.rooms.roombay.dto.response.ProfileCompletionResponse;
import org.rooms.roombay.service.AuthService;
import org.rooms.roombay.service.ProfileCompletionService;
import org.rooms.roombay.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "APIs for user authentication")
public class AuthController {

    private final AuthService authService;
    private final ProfileCompletionService profileCompletionService;
    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Register a new student user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Basic profile for the authenticated user (e.g. after OAuth redirect)")
    public ResponseEntity<AuthMeResponse> getMe() {
        var userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(authService.getAuthenticatedUser(userId));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update my account", description = "Update name, email, or phone. Changing email sends a new verification link and clears email verification until confirmed.")
    public ResponseEntity<AuthMeResponse> updateMe(@Valid @RequestBody UpdateMeRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(authService.updateMyAccount(userId, request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Login with email or phone and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for: {}", request.getEmailOrPhone());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String refreshToken = extractBearerToken(authHeader);
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate refresh session for the current token")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String refreshToken = extractBearerToken(authHeader);
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Logout completed")
                .build());
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify email address using token")
    public ResponseEntity<Object> verifyEmail(
            @RequestParam String token,
            @RequestHeader(value = "Accept", required = false) String acceptHeader) {
        boolean browserRequest = acceptHeader != null && acceptHeader.contains("text/html");
        try {
            authService.verifyEmailToken(token);
            if (browserRequest) {
                URI redirect = UriComponentsBuilder.fromUriString(frontendUrl)
                        .path("/account/verify")
                        .queryParam("emailStatus", "success")
                        .build(true)
                        .toUri();
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, redirect.toString())
                        .build();
            }
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Email verified successfully")
                    .build());
        } catch (RuntimeException ex) {
            if (browserRequest) {
                URI redirect = UriComponentsBuilder.fromUriString(frontendUrl)
                        .path("/account/verify")
                        .queryParam("emailStatus", "error")
                        .build(true)
                        .toUri();
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, redirect.toString())
                        .build();
            }
            throw ex;
        }
    }

    @PostMapping("/resend-verification-email")
    @Operation(summary = "Resend email verification", description = "Resend verification link to authenticated user")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail() {
        var userId = SecurityUtils.getCurrentUserId();
        authService.resendEmailVerification(userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Verification email sent")
                .build());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Request password reset")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password reset link sent to email")
                .build());
    }
    
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt");
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password reset successfully")
                .build());
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change password for authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        var userId = SecurityUtils.getCurrentUserId();
        log.info("Password change request for user: {}", userId);
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password changed successfully")
                .build());
    }

    @PostMapping("/deactivate-account")
    @Operation(summary = "Deactivate account", description = "Deactivate the currently authenticated user's account")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount() {
        var userId = SecurityUtils.getCurrentUserId();
        log.info("Deactivating account for user: {}", userId);
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Account deactivated successfully")
                .build());
    }

    @DeleteMapping("/delete-account")
    @Operation(summary = "Delete account", description = "Soft-delete the currently authenticated user's account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        var userId = SecurityUtils.getCurrentUserId();
        log.info("Deleting account for user: {}", userId);
        authService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Account deleted successfully")
                .build());
    }

    @GetMapping("/me/completion-status")
    @Operation(summary = "Get profile completion status", description = "Get role-specific completion score and missing steps")
    public ResponseEntity<ProfileCompletionResponse> getCompletionStatus() {
        var userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(profileCompletionService.getCompletionStatus(userId));
    }

    private String extractBearerToken(String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
