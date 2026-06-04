package org.rooms.roombay.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.service.AnalyticsEventService;
import org.rooms.roombay.service.AppErrorLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpsRetentionScheduler {

    private final AppErrorLogService appErrorLogService;
    private final AnalyticsEventService analyticsEventService;

    @Value("${roombay.ops.error-log-retention-days:60}")
    private int errorLogRetentionDays;

    @Value("${roombay.ops.analytics-retention-days:365}")
    private int analyticsRetentionDays;

    @Scheduled(cron = "${roombay.ops.retention-cron:0 30 2 * * *}")
    public void cleanupOpsTables() {
        int deletedErrors = appErrorLogService.deleteOlderThan(LocalDateTime.now().minusDays(errorLogRetentionDays));
        int deletedAnalytics = analyticsEventService.deleteOlderThan(LocalDateTime.now().minusDays(analyticsRetentionDays));
        if (deletedErrors > 0 || deletedAnalytics > 0) {
            log.info("Ops retention cleanup deleted appErrorLogs={} analyticsEvents={}", deletedErrors, deletedAnalytics);
        }
    }
}
