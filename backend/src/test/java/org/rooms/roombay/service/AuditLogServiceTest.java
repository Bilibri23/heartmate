package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.entity.AuditLog;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.repository.AuditLogRepository;
import org.rooms.roombay.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceTest {

    @Test
    void logAdminActionSanitizesSensitiveDetails() {
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogService service = new AuditLogService(auditLogRepository, userRepository);
        UUID adminId = UUID.randomUUID();
        User admin = User.builder().id(adminId).email("admin@example.com").role(User.UserRole.ADMIN).build();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        service.logAdminAction(
                adminId,
                "PAYMENT_PROOF_REJECTED",
                "Payment",
                UUID.randomUUID(),
                "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.abc.def password=secret https://res.cloudinary.com/demo/payment-proof/private.jpg"
        );

        var captor = forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails())
                .contains("[REDACTED]")
                .contains("[SENSITIVE_URL_REDACTED]")
                .doesNotContain("eyJhbGci")
                .doesNotContain("secret")
                .doesNotContain("payment-proof/private.jpg");
    }
}
