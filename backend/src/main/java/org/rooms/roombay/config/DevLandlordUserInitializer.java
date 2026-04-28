package org.rooms.roombay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Creates a password-based landlord account when explicitly enabled (local QA).
 * Set {@code app.dev.seed-landlord.enabled=true} once, verify login, then set back to false.
 */
@Component
@ConditionalOnProperty(name = "app.dev.seed-landlord.enabled", havingValue = "true")
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class DevLandlordUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.dev.seed-landlord.email:landlord-dev@roombay.local}")
    private String email;

    @Value("${app.dev.seed-landlord.password:ChangeMe123!}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isBlank()) {
            log.warn("app.dev.seed-landlord.email is blank; skip seeding");
            return;
        }
        if (userRepository.existsByEmail(normalized)) {
            log.info("Dev landlord seed skipped: email already exists ({})", normalized);
            return;
        }
        String phone = "dv" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        User user = User.builder()
                .email(normalized)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(password))
                .firstName("Dev")
                .lastName("Landlord")
                .gender(User.Gender.MALE)
                .role(User.UserRole.LANDLORD)
                .accountStatus(User.AccountStatus.PENDING)
                .emailVerified(true)
                .phoneVerified(false)
                .profileCompleted(false)
                .build();
        userRepository.save(user);
        log.warn(
                "Seeded dev landlord: email={} password=(app.dev.seed-landlord.password) — disable app.dev.seed-landlord.enabled after use",
                normalized);
    }
}
