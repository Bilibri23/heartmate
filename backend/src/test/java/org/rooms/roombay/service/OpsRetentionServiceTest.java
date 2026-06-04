package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpsRetentionServiceTest {

    @Test
    void appErrorLogRetentionDeletesOldRows() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AppErrorLogService service = new AppErrorLogService(jdbcTemplate);
        when(jdbcTemplate.update(eq("DELETE FROM app_error_log WHERE created_at < ?"), any(LocalDateTime.class)))
                .thenReturn(7);

        assertThat(service.deleteOlderThan(LocalDateTime.now().minusDays(60))).isEqualTo(7);
    }

    @Test
    void analyticsRetentionDeletesOldRows() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AnalyticsEventService service = new AnalyticsEventService(jdbcTemplate);
        when(jdbcTemplate.update(eq("DELETE FROM analytics_event WHERE created_at < ?"), any(LocalDateTime.class)))
                .thenReturn(11);

        assertThat(service.deleteOlderThan(LocalDateTime.now().minusDays(365))).isEqualTo(11);
    }
}
