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

@Component
@ConditionalOnProperty(name = "app.admin.seed.enabled", havingValue = "true")
@Order(101)
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed.email:}")
    private String email;

    @Value("${app.admin.seed.password:}")
    private String password;

    @Value("${app.admin.seed.first-name:Admin}")
    private String firstName;

    @Value("${app.admin.seed.last-name:User}")
    private String lastName;

    @Override
    public void run(ApplicationArguments args) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isBlank()) {
            log.warn("app.admin.seed.email is blank; skip admin seeding");
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("app.admin.seed.password is blank; skip admin seeding");
            return;
        }
        if (userRepository.existsByEmail(normalized)) {
            log.info("Admin seed skipped: email already exists ({)", normalized);
            return;
        }
        String phone = "ad" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        User user = User.builder()
                .email(normalized)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .gender(User.Gender.MALE)
                .role(User.UserRole.ADMIN)
                .accountStatus(User.AccountStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .profileCompleted(true)
                .build();
        userRepository.save(user);
        log.warn("Seeded admin user: email={} -- disable app.admin.seed.enabled after use", normalized);
    }
}
