package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppErrorLogServiceTest {

    @Test
    void sanitizerRedactsTokensCookiesPasswordsAndSecrets() {
        String sanitized = AppErrorLogService.sanitizeForOpsLog(
                "Authorization: Bearer abc.def.ghi password=secret Cookie=session jwt=token refresh_token=rotatedvalue api_key=cloud");

        assertThat(sanitized).contains("[REDACTED]");
        assertThat(sanitized).contains("password=[REDACTED]");
        assertThat(sanitized).contains("Cookie=[REDACTED]");
        assertThat(sanitized).contains("jwt=[REDACTED]");
        assertThat(sanitized).contains("refresh_token=[REDACTED]");
        assertThat(sanitized).contains("api_key=[REDACTED]");
        assertThat(sanitized).doesNotContain("abc.def.ghi", "secret", "rotatedvalue", "cloud");
    }
}
