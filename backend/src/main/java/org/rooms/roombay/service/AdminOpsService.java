package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.ai.rag.AiGraphRagService;
import org.rooms.roombay.config.AdvancedRateLimitInterceptor;
import org.rooms.roombay.dto.response.AiGraphStatsResponse;
import org.rooms.roombay.entity.LandlordVerification;
import org.rooms.roombay.entity.Payment;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.Report;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.StudentVerification;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.PaymentRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.ReportRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOpsService {

    private final DataSource dataSource;
    private final AiGraphRagService aiGraphRagService;
    private final AiIngestRunService aiIngestRunService;
    private final AppErrorLogService appErrorLogService;
    private final AnalyticsEventService analyticsEventService;
    private final PropertyListingRepository listingRepository;
    private final StudentVerificationRepository studentVerificationRepository;
    private final LandlordVerificationRepository landlordVerificationRepository;
    private final PaymentRepository paymentRepository;
    private final ReportRepository reportRepository;
    private final RoomApplicationRepository roomApplicationRepository;
    private final AdvancedRateLimitInterceptor rateLimitInterceptor;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${railway_git_commit_sha:${GIT_COMMIT:${SOURCE_VERSION:unknown}}}")
    private String deployVersion;

    @Value("${roombay.backup.last-verified-at:}")
    private String backupLastVerifiedAt;

    @Value("${roombay.backup.rpo-hours:24}")
    private int backupRpoHours;

    public Map<String, Object> health() {
        boolean databaseUp = databaseUp();
        AiGraphStatsResponse graphStats = safeGraphStats();
        Map<String, Object> lastIngest = safeLastSuccessfulIngest();
        long websocketFailures = appErrorLogService.countBySourceSince("WEBSOCKET", LocalDateTime.now().minusHours(1));

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("backendStatus", "UP");
        health.put("databaseStatus", databaseUp ? "UP" : "DOWN");
        health.put("websocketStatus", websocketFailures > 0 ? "DEGRADED" : "UP");
        health.put("aiIngestionStatus", lastIngest != null ? "READY" : "MISSING");
        health.put("aiDocsCount", graphStats.getDocumentCount());
        health.put("aiChunksCount", graphStats.getChunkCount());
        health.put("graphRagEntityCount", graphStats.getEntityCount());
        health.put("graphRagEdgeCount", graphStats.getEdgeCount());
        health.put("lastSuccessfulAiIngestTime", lastIngest != null ? lastIngest.get("finishedAt") : null);
        health.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        health.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        health.put("environment", activeProfile);
        health.put("deployVersion", deployVersion);
        health.put("rateLimitMode", rateLimitInterceptor.getCurrentMode().name());
        health.put("backupLastVerifiedAt", backupLastVerifiedAt.isBlank() ? null : backupLastVerifiedAt);
        health.put("backupRpoHours", backupRpoHours);
        health.put("timestamp", Instant.now().toString());
        return health;
    }

    public List<Map<String, Object>> recentErrors(int limit) {
        return appErrorLogService.recent(limit);
    }

    public Map<String, Object> funnel(String range) {
        LocalDateTime since = since(range);
        Map<String, Long> counts = analyticsEventService.countsByTypeSince(since);

        Map<String, Object> funnel = new LinkedHashMap<>();
        funnel.put("range", normalizeRange(range));
        funnel.put("since", since.toString());
        funnel.put("counts", Map.ofEntries(
                Map.entry("feed_opened", counts.getOrDefault("feed_opened", 0L)),
                Map.entry("search_performed", counts.getOrDefault("search_performed", 0L)),
                Map.entry("listing_view", counts.getOrDefault("listing_view", 0L)),
                Map.entry("save_listing", counts.getOrDefault("save_listing", 0L)),
                Map.entry("application_started", counts.getOrDefault("application_started", 0L)),
                Map.entry("application_submitted", counts.getOrDefault("application_submitted", 0L)),
                Map.entry("application_approved", counts.getOrDefault("application_approved", 0L)),
                Map.entry("application_rejected", counts.getOrDefault("application_rejected", 0L)),
                Map.entry("lease_generated", counts.getOrDefault("lease_generated", 0L)),
                Map.entry("lease_signed", counts.getOrDefault("lease_signed", 0L)),
                Map.entry("payment_proof_uploaded", counts.getOrDefault("payment_proof_uploaded", 0L)),
                Map.entry("listing_approved", counts.getOrDefault("listing_approved", 0L)),
                Map.entry("listing_rejected", counts.getOrDefault("listing_rejected", 0L))
        ));
        funnel.put("conversionRates", Map.of(
                "listing_view_to_save", conversionRate(counts.getOrDefault("save_listing", 0L), counts.getOrDefault("listing_view", 0L)),
                "listing_view_to_application_submitted", conversionRate(counts.getOrDefault("application_submitted", 0L), counts.getOrDefault("listing_view", 0L)),
                "application_submitted_to_approved", conversionRate(counts.getOrDefault("application_approved", 0L), counts.getOrDefault("application_submitted", 0L)),
                "approved_to_lease_signed", conversionRate(counts.getOrDefault("lease_signed", 0L), counts.getOrDefault("application_approved", 0L)),
                "lease_signed_to_payment_proof_uploaded", conversionRate(counts.getOrDefault("payment_proof_uploaded", 0L), counts.getOrDefault("lease_signed", 0L))
        ));
        return funnel;
    }

    public List<Map<String, Object>> alerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> health = health();
        AiGraphStatsResponse graphStats = safeGraphStats();

        if ("DOWN".equals(health.get("databaseStatus"))) {
            alerts.add(alert("CRITICAL", "database_down", "Database down", "The database health check failed.", "databaseStatus", "UP", "Inspect Railway Postgres connectivity and recent deployment logs."));
        }

        long recent5xx = appErrorLogService.countRecentServerErrors(now.minusHours(1));
        if (recent5xx >= 20) {
            alerts.add(alert("CRITICAL", "high_5xx_errors", "High 5xx errors", recent5xx + " server errors were logged in the last hour.", "recent5xx", 20, "Open recent errors and Railway logs; prioritize database and AI failures."));
        } else if (recent5xx >= 5) {
            alerts.add(alert("WARNING", "high_5xx_errors", "Elevated 5xx errors", recent5xx + " server errors were logged in the last hour.", "recent5xx", 5, "Review recent errors for repeated sources or paths."));
        }

        if (safeLastSuccessfulIngest() == null || graphStats.getDocumentCount() == 0 || graphStats.getChunkCount() == 0) {
            alerts.add(alert("WARNING", "ai_ingest_missing", "AI ingest missing", "RoomBay AI has no successful ingest run or no indexed docs/chunks.", "aiDocsCount", 1, "Run admin doc ingest with force=true and check AI_DOCS_DIR packaging."));
        }

        long aiNoAnswer = analyticsEventService.countSince("ai_no_answer", now.minusHours(24));
        if (aiNoAnswer >= 10) {
            alerts.add(alert("WARNING", "ai_no_answer_spike", "AI no-answer spike", aiNoAnswer + " no-answer AI events were recorded in the last 24 hours.", "aiNoAnswer24h", 10, "Review AI analytics and expand docs for the missed workflows."));
        }

        long websocketFailures = appErrorLogService.countBySourceSince("WEBSOCKET", now.minusHours(1));
        if (websocketFailures > 0) {
            alerts.add(alert("WARNING", "websocket_auth_failures", "WebSocket auth failures", websocketFailures + " WebSocket failures were logged in the last hour.", "websocketFailures1h", 1, "Check token handling, SockJS handshake status, and CORS/origin config."));
        }

        long activeListings = safeActiveListingsCount();
        if (activeListings < 5) {
            alerts.add(alert("INFO", "low_listing_supply", "Low listing supply", "Only " + activeListings + " active listings are currently available.", "activeListings", 5, "Review pending listings and landlord onboarding."));
        }

        long pendingVerifications = safePendingVerificationCount();
        if (pendingVerifications > 20) {
            alerts.add(alert("WARNING", "pending_verifications_high", "Pending verifications high", pendingVerifications + " tenant and landlord verification items are pending.", "pendingVerifications", 20, "Prioritize verification review queues."));
        }

        long pendingListings = safeCount(() -> listingRepository.findPendingListings().size());
        if (pendingListings > 20) {
            alerts.add(alert("WARNING", "pending_listing_reviews_high", "Pending listing reviews high", pendingListings + " listings are awaiting review.", "pendingListings", 20, "Review listing approval queue and prioritize older submissions."));
        }

        long pendingReports = safeCount(() -> {
            Long count = reportRepository.countByStatus(Report.ReportStatus.PENDING);
            return count != null ? count : 0;
        });
        if (pendingReports > 10) {
            alerts.add(alert("WARNING", "pending_reports_high", "Pending reports high", pendingReports + " user reports are open.", "pendingReports", 10, "Open reports queue and resolve high-risk reports first."));
        }

        long paymentProofs = safeCount(() -> paymentRepository.countByStatus(Payment.PaymentStatus.SUBMITTED));
        if (paymentProofs > 10) {
            alerts.add(alert("WARNING", "payment_proof_queue_high", "Payment proof queue high", paymentProofs + " payment proofs are awaiting admin review.", "pendingPaymentProofs", 10, "Review submitted payment proofs and flag duplicates."));
        }

        if (rateLimitInterceptor.getCurrentMode() == AdvancedRateLimitInterceptor.RateLimitMode.UNAVAILABLE) {
            alerts.add(alert("CRITICAL", "rate_limiting_unavailable", "Rate limiting unavailable", "Redis-backed rate limiting is required but currently unavailable.", "rateLimitMode", "REDIS", "Check Redis service variables and connectivity before scaling traffic."));
        }

        if (backupLastVerifiedAt == null || backupLastVerifiedAt.isBlank()) {
            alerts.add(alert("WARNING", "backup_restore_drill_missing", "Backup restore drill missing", "No backup restore verification timestamp is configured.", "backupLastVerifiedAt", "configured", "Run the database restore drill and set ROOMBAY_BACKUP_LAST_VERIFIED_AT."));
        }

        return alerts;
    }

    public Map<String, Object> aiHealth() {
        AiGraphStatsResponse stats = safeGraphStats();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("docs", stats.getDocumentCount());
        data.put("chunks", stats.getChunkCount());
        data.put("entities", stats.getEntityCount());
        data.put("edges", stats.getEdgeCount());
        data.put("lastIngest", safeLastSuccessfulIngest());
        data.put("aiNoAnswerCount", analyticsEventService.countSince("ai_no_answer", LocalDateTime.now().minusHours(24)));
        return data;
    }

    public Map<String, Object> queues() {
        Map<String, Object> queues = new LinkedHashMap<>();
        queues.put("pendingTenantVerifications", safeCount(() -> studentVerificationRepository.countByStatus(StudentVerification.Status.PENDING)));
        queues.put("pendingLandlordVerifications", safeCount(() -> landlordVerificationRepository.findAllPendingVerifications().size()));
        queues.put("pendingListings", safeCount(() -> listingRepository.findPendingListings().size()));
        queues.put("pendingReports", safeCount(() -> {
            Long count = reportRepository.countByStatus(Report.ReportStatus.PENDING);
            return count != null ? count : 0;
        }));
        queues.put("pendingPaymentProofs", safeCount(() -> paymentRepository.countByStatus(Payment.PaymentStatus.SUBMITTED)));
        queues.put("staleApplications", safeCount(() -> roomApplicationRepository.findExpiredApplications(LocalDateTime.now()).stream()
                .filter(RoomApplication::isActive)
                .count()));
        queues.put("staleLandlordResponses", safeCount(() -> roomApplicationRepository.countByStatus(RoomApplication.Status.PENDING)));
        queues.put("timestamp", Instant.now().toString());
        return queues;
    }

    public List<Map<String, Object>> topNoAnswerQuestions(String range, int limit) {
        return analyticsEventService.topNoAnswerQuestions(since(range), limit);
    }

    static double conversionRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 10000.0 / denominator)) / 100.0;
    }

    private boolean databaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ex) {
            appErrorLogService.log("ERROR", "DATABASE", "Database health check failed: " + ex.getMessage(), "/api/admin/ops/health", ex);
            return false;
        }
    }

    private AiGraphStatsResponse safeGraphStats() {
        try {
            return aiGraphRagService.getGraphStats();
        } catch (Exception ex) {
            appErrorLogService.log("ERROR", "AI", "AI graph stats failed: " + ex.getMessage(), "/api/admin/ops/health", ex);
            return AiGraphStatsResponse.builder().build();
        }
    }

    private Map<String, Object> safeLastSuccessfulIngest() {
        try {
            return aiIngestRunService.lastSuccessful();
        } catch (Exception ex) {
            return null;
        }
    }

    private long safeActiveListingsCount() {
        try {
            return listingRepository.countByStatus(PropertyListing.Status.ACTIVE);
        } catch (Exception ex) {
            return 0;
        }
    }

    private long safePendingVerificationCount() {
        try {
            long tenants = studentVerificationRepository.countByStatus(StudentVerification.Status.PENDING);
            long landlords = landlordVerificationRepository.findAllPendingVerifications().size();
            return tenants + landlords;
        } catch (Exception ex) {
            return 0;
        }
    }

    private long safeCount(CountSupplier supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface CountSupplier {
        long get();
    }

    private static LocalDateTime since(String range) {
        return switch (normalizeRange(range)) {
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "90d" -> LocalDateTime.now().minusDays(90);
            case "30d" -> LocalDateTime.now().minusDays(30);
            default -> LocalDateTime.now().minusDays(7);
        };
    }

    private static String normalizeRange(String range) {
        if ("24h".equalsIgnoreCase(range) || "30d".equalsIgnoreCase(range) || "90d".equalsIgnoreCase(range)) {
            return range.toLowerCase();
        }
        return "7d";
    }

    private Map<String, Object> alert(String severity, String code, String title, String message, String metric, Object threshold, String suggestedAction) {
        return Map.of(
                "severity", severity,
                "code", code,
                "title", title,
                "message", message,
                "metric", metric,
                "threshold", threshold,
                "createdAt", Instant.now().toString(),
                "suggestedAction", suggestedAction
        );
    }
}
