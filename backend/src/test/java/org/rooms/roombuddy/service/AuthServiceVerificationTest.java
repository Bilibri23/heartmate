package org.rooms.roombay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.request.LoginRequest;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.repository.EmailVerificationTokenRepository;
import org.rooms.roombay.repository.PasswordResetTokenRepository;
import org.rooms.roombay.repository.RefreshTokenSessionRepository;
import org.rooms.roombay.repository.UserRepository;
import org.rooms.roombay.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceVerificationTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock
    private RefreshTokenSessionRepository refreshTokenSessionRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .phone("+237612345678")
                .passwordHash("hashed")
                .role(User.UserRole.STUDENT)
                .accountStatus(User.AccountStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(true)
                .build();
    }

    @Test
    void loginFailsWhenEmailNotVerified() {
        LoginRequest request = new LoginRequest();
        request.setEmailOrPhone(user.getEmail());
        request.setPassword("password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    void loginFailsWhenPhoneNotVerified() {
        user.setEmailVerified(true);
        user.setPhoneVerified(false);

        LoginRequest request = new LoginRequest();
        request.setEmailOrPhone(user.getEmail());
        request.setPassword("password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }
}
