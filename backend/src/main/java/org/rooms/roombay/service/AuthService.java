package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.LoginRequest;
import org.rooms.roombay.dto.request.RegisterRequest;
import org.rooms.roombay.dto.request.ResetPasswordRequest;
import org.rooms.roombay.dto.request.ChangePasswordRequest;
import org.rooms.roombay.dto.response.AuthMeResponse;
import org.rooms.roombay.dto.response.AuthResponse;
import org.rooms.roombay.entity.PasswordResetToken;
import org.rooms.roombay.entity.EmailVerificationToken;
import org.rooms.roombay.entity.RefreshTokenSession;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.PasswordResetTokenRepository;
import org.rooms.roombay.repository.EmailVerificationTokenRepository;
import org.rooms.roombay.repository.RefreshTokenSessionRepository;
import org.rooms.roombay.repository.UserRepository;
import org.rooms.roombay.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
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
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final EmailService emailService;

    /** When false, login succeeds without email/phone verification (remind later in UI). */
    @Value("${app.verification.require-for-login:false}")
    private boolean verificationRequiredForLogin;

    @Value("${app.email.send-verification-on-register:false}")
    private boolean sendVerificationEmailOnRegister;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final int TOKEN_EXPIRATION_HOURS = 1;
    private static final int EMAIL_VERIFICATION_EXPIRATION_HOURS = 24;

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
        if (sendVerificationEmailOnRegister) {
            sendEmailVerification(savedUser);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
        String refreshToken = issueRefreshToken(savedUser);

        return buildAuthResponse(savedUser, accessToken, refreshToken);
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

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BadRequestException("This account uses Google sign-in.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        // Check if account is active
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE &&
                user.getAccountStatus() != User.AccountStatus.PENDING) {
            throw new BadRequestException("Account is " + user.getAccountStatus());
        }
        if (verificationRequiredForLogin) {
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                throw new BadRequestException("Please verify your email before logging in.");
            }
            if (!Boolean.TRUE.equals(user.getPhoneVerified())) {
                throw new BadRequestException("Please verify your phone number before logging in.");
            }
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
        String refreshToken = issueRefreshToken(user);

        log.info("User logged in successfully: {}", user.getId());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthMeResponse getAuthenticatedUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return AuthMeResponse.from(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }
        if (!"refresh".equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new BadRequestException("Invalid token type for refresh");
        }

        UUID tokenId = jwtTokenProvider.getTokenId(refreshToken);
        RefreshTokenSession session = refreshTokenSessionRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new BadRequestException("Refresh session not found"));
        if (Boolean.TRUE.equals(session.getRevoked()) || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token has been revoked or expired");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        session.setRevoked(true);
        refreshTokenSessionRepository.save(session);

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String newRefreshToken = issueRefreshToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
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

    public void verifyEmailToken(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));
        if (!verificationToken.isValid()) {
            throw new BadRequestException("Verification token is expired or already used");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);
    }

    public void resendEmailVerification(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }
        sendEmailVerification(user);
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

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BadRequestException("Set a password first or use Google sign-in.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenSessionRepository.revokeAllActiveByUserId(userId);

        log.info("Password changed successfully for user: {}", userId);
    }

    public void deactivateAccount(UUID userId) {
        log.info("Deactivating account for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setAccountStatus(User.AccountStatus.DEACTIVATED);
        userRepository.save(user);
        refreshTokenSessionRepository.revokeAllActiveByUserId(userId);

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
        refreshTokenSessionRepository.revokeAllActiveByUserId(userId);

        log.info("Account soft-deleted for user: {}", userId);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return;
        }
        if (!"refresh".equals(jwtTokenProvider.getTokenType(refreshToken))) {
            return;
        }

        UUID tokenId = jwtTokenProvider.getTokenId(refreshToken);
        refreshTokenSessionRepository.findByTokenId(tokenId).ifPresent(session -> {
            session.setRevoked(true);
            refreshTokenSessionRepository.save(session);
        });
    }

    private String issueRefreshToken(User user) {
        UUID tokenId = UUID.randomUUID();
        // Do not set id on new sessions: a non-null id makes JpaRepository.save() use merge(),
        // which caused StaleObjectStateException for inserts. Let @GeneratedValue assign id.
        RefreshTokenSession session = refreshTokenSessionRepository.save(
                RefreshTokenSession.builder()
                        .user(user)
                        .tokenId(tokenId)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .revoked(false)
                        .build()
        );
        return jwtTokenProvider.generateRefreshToken(user.getId(), session.getId(), tokenId);
    }

    /**
     * Google OAuth: find or create user, issue JWTs. Email from Google is treated as verified.
     */
    public AuthResponse authenticateWithGoogle(String subject, String email, String firstName, String lastName) {
        if (subject == null || subject.isBlank()) {
            throw new BadRequestException("Invalid OAuth subject");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google did not return an email address.");
        }
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByOauthProviderAndOauthSubject("GOOGLE", subject).orElse(null);
        if (user != null) {
            user.setLastActive(LocalDateTime.now());
            user = userRepository.save(user);
        } else {
            Optional<User> byEmail = userRepository.findByEmail(normalizedEmail);
            if (byEmail.isPresent()) {
                user = byEmail.get();
                if (user.getOauthSubject() != null && !subject.equals(user.getOauthSubject())) {
                    throw new BadRequestException("This email is linked to another sign-in method.");
                }
                user.setOauthProvider("GOOGLE");
                user.setOauthSubject(subject);
                user.setEmailVerified(true);
                user.setLastActive(LocalDateTime.now());
                user = userRepository.save(user);
            } else {
                String phonePlaceholder = "og" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
                String fn = (firstName != null && !firstName.isBlank()) ? firstName.trim() : "User";
                String ln = (lastName != null && !lastName.isBlank()) ? lastName.trim() : "";
                user = User.builder()
                        .email(normalizedEmail)
                        .phone(phonePlaceholder)
                        .passwordHash(null)
                        .firstName(fn)
                        .lastName(ln)
                        .role(User.UserRole.STUDENT)
                        .accountStatus(User.AccountStatus.PENDING)
                        .emailVerified(true)
                        .phoneVerified(false)
                        .profileCompleted(false)
                        .oauthProvider("GOOGLE")
                        .oauthSubject(subject)
                        .build();
                user = userRepository.save(user);
                log.info("Registered new user via Google OAuth: {}", user.getId());
            }
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = issueRefreshToken(user);
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L)
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .build();
    }

    private void sendEmailVerification(User user) {
        emailVerificationTokenRepository.markAllUsedByUserId(user.getId());
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(EMAIL_VERIFICATION_EXPIRATION_HOURS))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(verificationToken);
        emailService.sendEmailVerificationEmail(user.getEmail(), token);
    }
}
