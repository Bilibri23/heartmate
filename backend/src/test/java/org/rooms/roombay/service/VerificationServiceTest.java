package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.rooms.roombay.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerificationServiceTest {

    private final StudentVerificationRepository verificationRepository = mock(StudentVerificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    private final VerificationService service = new VerificationService(
            verificationRepository,
            userRepository,
            emailService,
            notificationService,
            auditLogService
    );

    @Test
    void getVerificationOrNoneReturnsNoneForFirstTimeUsers() {
        UUID userId = UUID.randomUUID();
        when(verificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var response = service.getVerificationOrNone(userId);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStatus()).isEqualTo("NONE");
        assertThat(response.getId()).isNull();
    }
}
