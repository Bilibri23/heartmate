package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.LoginRequest;
import org.rooms.roombuddy.dto.request.RegisterRequest;
import org.rooms.roombuddy.dto.request.ResetPasswordRequest;
import org.rooms.roombuddy.dto.request.ChangePasswordRequest;
import org.rooms.roombuddy.dto.response.AuthResponse;
import org.rooms.roombuddy.entity.PasswordResetToken;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.PasswordResetTokenRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.rooms.roombuddy.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final int TOKEN_EXPIRATION_HOURS = 1;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Check if phone already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number already registered");
        }

        // Determine role - default to STUDENT if not provided or invalid
        User.UserRole userRole = User.UserRole.STUDENT;
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                userRole = User.UserRole.valueOf(request.getRole().toUpperCase());
                // Only allow STUDENT or LANDLORD during registration (ADMIN is created manually)
                if (userRole == User.UserRole.ADMIN) {
                    userRole = User.UserRole.STUDENT;
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided: {}, defaulting to STUDENT", request.getRole());
                userRole = User.UserRole.STUDENT;
            }
        }

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .gender(User.Gender.valueOf(request.getGender()))
                .dateOfBirth(request.getDateOfBirth())
                .role(userRole)
                .accountStatus(User.AccountStatus.PENDING)
                .emailVerified(false)
                .phoneVerified(false)
                .profileCompleted(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId());

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L) // 1 hour in seconds
                .role(savedUser.getRole().name())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmailOrPhone());

        // Determine if email or phone
        User user;
        if (EMAIL_PATTERN.matcher(request.getEmailOrPhone()).matches()) {
            user = userRepository.findByEmail(request.getEmailOrPhone())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));
        } else {
            user = userRepository.findByPhone(request.getEmailOrPhone())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        // Check if account is active
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE &&
                user.getAccountStatus() != User.AccountStatus.PENDING) {
            throw new BadRequestException("Account is " + user.getAccountStatus());
        }

        // Update last active
        user.setLastActive(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("User logged in successfully: {}", user.getId());

        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L)
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    public AuthResponse  refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(3600L)
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
    
    public void requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // Invalidate all existing tokens for this user
        passwordResetTokenRepository.markAllTokensAsUsedByUserId(user.getId());
        
        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .used(false)
                .build();
        
        passwordResetTokenRepository.save(resetToken);
        
        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), token);
        
        log.info("Password reset token generated and email sent for user: {}", user.getId());
    }
    
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Password reset attempt with token: {}", request.getToken());
        
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        
        // Validate token
        if (!resetToken.isValid()) {
            throw new BadRequestException("Invalid or expired reset token");
        }
        
        // Update password
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        
        log.info("Password reset successfully for user: {}", user.getId());
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);
    }

    public void deactivateAccount(UUID userId) {
        log.info("Deactivating account for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setAccountStatus(User.AccountStatus.DEACTIVATED);
        userRepository.save(user);

        log.info("Account deactivated for user: {}", userId);
    }

    public void deleteAccount(UUID userId) {
        log.info("Soft-deleting account for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String suffix = user.getId().toString();
        user.setEmail("deleted+" + suffix + "@roomconnect.local");
        user.setPhone("DELETED_" + suffix);
        user.setAccountStatus(User.AccountStatus.DEACTIVATED);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setProfileCompleted(false);

        userRepository.save(user);

        log.info("Account soft-deleted for user: {}", userId);
    }
}
