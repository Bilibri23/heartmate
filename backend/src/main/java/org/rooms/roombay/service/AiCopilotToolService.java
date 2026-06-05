package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.entity.LandlordVerification;
import org.rooms.roombay.entity.Payment;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.Report;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.StudentVerification;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.LeaseRepository;
import org.rooms.roombay.repository.ListingFavoriteRepository;
import org.rooms.roombay.repository.PaymentRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.ReportRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCopilotToolService {

    private final StudentVerificationRepository studentVerificationRepository;
    private final RoomApplicationRepository roomApplicationRepository;
    private final LeaseRepository leaseRepository;
    private final ListingFavoriteRepository listingFavoriteRepository;
    private final PaymentRepository paymentRepository;
    private final PropertyListingRepository propertyListingRepository;
    private final LandlordVerificationRepository landlordVerificationRepository;
    private final ReportRepository reportRepository;
    private final AppErrorLogService appErrorLogService;
    private final AnalyticsEventService analyticsEventService;
    private final JdbcTemplate jdbcTemplate;

    public String buildToolContext(UUID userId, String role) {
        String normalizedRole = normalizeRole(role);
        List<String> lines = new ArrayList<>();
        if ("ADMIN".equals(normalizedRole)) {
            addTool(lines, userId, normalizedRole, "admin_ops_snapshot", this::adminOpsSnapshot);
            addTool(lines, userId, normalizedRole, "admin_recent_errors", this::adminRecentErrors);
            addTool(lines, userId, normalizedRole, "admin_ai_unanswered_questions", this::adminAiUnansweredQuestions);
            addTool(lines, userId, normalizedRole, "admin_funnel_summary", this::adminFunnelSummary);
        } else if ("LANDLORD".equals(normalizedRole)) {
            addTool(lines, userId, normalizedRole, "landlord_portfolio_status", () -> landlordPortfolioStatus(userId));
            addTool(lines, userId, normalizedRole, "landlord_listing_performance", () -> landlordListingPerformance(userId));
        } else {
            addTool(lines, userId, normalizedRole, "tenant_status_snapshot", () -> tenantStatusSnapshot(userId));
        }
        return lines.isEmpty() ? "" : "Read_only_platform_tools:\n" + String.join("\n", lines);
    }

    public boolean isToolAllowed(String role, String toolName) {
        String normalizedRole = normalizeRole(role);
        String normalizedTool = toolName == null ? "" : toolName.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedRole) {
            case "ADMIN" -> normalizedTool.startsWith("admin_");
            case "LANDLORD" -> normalizedTool.startsWith("landlord_");
            default -> normalizedTool.startsWith("tenant_");
        };
    }

    private String tenantStatusSnapshot(UUID userId) {
        String verification = studentVerificationRepository.findByUserId(userId)
                .map(v -> v.getStatus().name())
                .orElse("NONE");
        long applications = roomApplicationRepository.countByStudentId(userId);
        long pendingApplications = roomApplicationRepository.countByStudentIdAndStatus(userId, RoomApplication.Status.PENDING);
        long acceptedApplications = roomApplicationRepository.countByStudentIdAndStatus(userId, RoomApplication.Status.ACCEPTED);
        long leases = leaseRepository.countByStudentId(userId);
        long payments = paymentRepository.countByPayerId(userId);
        long pendingPayments = paymentRepository.countByPayerIdAndStatus(userId, Payment.PaymentStatus.PENDING);
        long submittedPayments = paymentRepository.countByPayerIdAndStatus(userId, Payment.PaymentStatus.SUBMITTED);
        long verifiedPayments = paymentRepository.countByPayerIdAndStatus(userId, Payment.PaymentStatus.VERIFIED);
        long savedListings = listingFavoriteRepository.findByUserId(userId).size();
        return "tenantStatusSnapshot={verificationStatus=%s, applicationsTotal=%d, applicationsPending=%d, applicationsAccepted=%d, leasesTotal=%d, paymentsTotal=%d, paymentsPending=%d, paymentsSubmitted=%d, paymentsVerified=%d, savedListings=%d}"
                .formatted(verification, applications, pendingApplications, acceptedApplications, leases, payments, pendingPayments, submittedPayments, verifiedPayments, savedListings);
    }

    private String landlordPortfolioStatus(UUID userId) {
        String verification = landlordVerificationRepository.findByUserId(userId)
                .map(v -> "identity=%s,business=%s,property=%s,trustScore=%s"
                        .formatted(v.getIdentityStatus(), v.getBusinessStatus(), v.getPropertyStatus(), safeNumber(v.getTrustScore())))
                .orElse("NONE");
        long listings = propertyListingRepository.countByLandlordId(userId);
        long verifiedListings = propertyListingRepository.countByLandlordIdAndVerifiedTrue(userId);
        long pendingApplications = roomApplicationRepository.countByLandlordIdAndStatus(userId, RoomApplication.Status.PENDING);
        long leases = leaseRepository.countByLandlordId(userId);
        long payments = paymentRepository.countByRecipientId(userId);
        long submittedProofs = paymentRepository.countByRecipientIdAndStatus(userId, Payment.PaymentStatus.SUBMITTED);
        long verifiedPayments = paymentRepository.countByRecipientIdAndStatus(userId, Payment.PaymentStatus.VERIFIED);
        return "landlordPortfolioStatus={verification=%s, listingsTotal=%d, listingsVerified=%d, applicationsPending=%d, leasesTotal=%d, paymentRecords=%d, paymentProofsSubmitted=%d, paymentsVerified=%d}"
                .formatted(verification, listings, verifiedListings, pendingApplications, leases, payments, submittedProofs, verifiedPayments);
    }

    private String landlordListingPerformance(UUID userId) {
        List<PropertyListing> listings = propertyListingRepository.findByLandlordId(userId);
        long active = listings.stream().filter(l -> l.getStatus() == PropertyListing.Status.ACTIVE).count();
        long pending = listings.stream().filter(l -> l.getStatus() == PropertyListing.Status.PENDING).count();
        long totalViews = listings.stream().mapToLong(l -> l.getViewsCount() == null ? 0 : l.getViewsCount()).sum();
        long totalFavorites = listings.stream().mapToLong(l -> l.getFavoritesCount() == null ? 0 : l.getFavoritesCount()).sum();
        return "landlordListingPerformance={activeListings=%d, pendingReviewListings=%d, totalViews=%d, totalFavorites=%d}"
                .formatted(active, pending, totalViews, totalFavorites);
    }

    private String adminOpsSnapshot() {
        long pendingStudentVerifications = studentVerificationRepository.countByStatus(StudentVerification.Status.PENDING);
        long pendingLandlordVerifications =
                landlordVerificationRepository.countByIdentityStatus(LandlordVerification.VerificationStatus.PENDING)
                        + landlordVerificationRepository.countByBusinessStatus(LandlordVerification.VerificationStatus.PENDING)
                        + landlordVerificationRepository.countByPropertyStatus(LandlordVerification.VerificationStatus.PENDING);
        long pendingListings = propertyListingRepository.countByStatus(PropertyListing.Status.PENDING);
        long pendingPaymentProofs = paymentRepository.countByStatus(Payment.PaymentStatus.SUBMITTED);
        long pendingReports = safeCountReports(Report.ReportStatus.PENDING)
                + safeCountReports(Report.ReportStatus.OPEN)
                + safeCountReports(Report.ReportStatus.UNDER_REVIEW);
        return "adminOpsSnapshot={pendingStudentVerifications=%d, pendingLandlordVerificationSections=%d, pendingListings=%d, pendingPaymentProofs=%d, pendingReports=%d}"
                .formatted(pendingStudentVerifications, pendingLandlordVerifications, pendingListings, pendingPaymentProofs, pendingReports);
    }

    private String adminRecentErrors() {
        List<Map<String, Object>> recent = appErrorLogService.recent(5);
        String summary = recent.stream()
                .map(e -> "%s:%s:%s".formatted(
                        safeValue(e.get("level")),
                        safeValue(e.get("source")),
                        truncate(safeValue(e.get("path")), 80)))
                .toList()
                .toString();
        return "adminRecentErrors={count=%d, latest=%s}".formatted(recent.size(), summary);
    }

    private String adminAiUnansweredQuestions() {
        List<Map<String, Object>> questions = analyticsEventService.topNoAnswerQuestions(LocalDateTime.now().minusDays(7), 5);
        String summary = questions.stream()
                .map(q -> "%s x%s".formatted(truncate(safeValue(q.get("question")), 90), safeValue(q.get("count"))))
                .toList()
                .toString();
        return "adminAiUnansweredQuestions={range=7d, count=%d, top=%s}".formatted(questions.size(), summary);
    }

    private String adminFunnelSummary() {
        Map<String, Long> counts = analyticsEventService.countsByTypeSince(LocalDateTime.now().minusDays(7));
        return "adminFunnelSummary={range=7d, feedOpened=%d, searches=%d, listingViews=%d, saves=%d, applicationsSubmitted=%d, applicationsApproved=%d, leasesSigned=%d}"
                .formatted(
                        counts.getOrDefault("feed_opened", 0L),
                        counts.getOrDefault("search_performed", 0L),
                        counts.getOrDefault("listing_view", 0L),
                        counts.getOrDefault("save_listing", 0L),
                        counts.getOrDefault("application_submitted", 0L),
                        counts.getOrDefault("application_approved", 0L),
                        counts.getOrDefault("lease_signed", 0L)
                );
    }

    private void addTool(List<String> lines, UUID userId, String role, String toolName, ToolSupplier supplier) {
        if (!isToolAllowed(role, toolName)) {
            logToolCall(userId, role, toolName, false, "blocked_by_role_policy");
            return;
        }
        try {
            String result = supplier.get();
            lines.add(result);
            logToolCall(userId, role, toolName, true, result);
        } catch (Exception e) {
            log.warn("[AI] read-only tool failed tool={} message={}", toolName, e.getMessage());
            logToolCall(userId, role, toolName, true, "tool_failed");
        }
    }

    private long safeCountReports(Report.ReportStatus status) {
        Long count = reportRepository.countByStatus(status);
        return count == null ? 0 : count;
    }

    private void logToolCall(UUID userId, String role, String toolName, boolean allowed, String resultSummary) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO ai_tool_call_log (user_id, role, tool_name, allowed, result_summary)
                    VALUES (?, ?, ?, ?, ?)
                    """, userId, role, toolName, allowed, truncate(resultSummary, 600));
        } catch (Exception e) {
            log.debug("[AI] could not record tool call: {}", e.getMessage());
        }
    }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) return "STUDENT";
        String normalized = role.toUpperCase(Locale.ROOT).replace("ROLE_", "");
        return "TENANT".equals(normalized) ? "STUDENT" : normalized;
    }

    private static String safeNumber(Number n) {
        return n == null ? "NONE" : n.toString();
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        String safe = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return safe.length() <= max ? safe : safe.substring(0, max);
    }

    private static String safeValue(Object value) {
        if (value == null) return "NONE";
        return AppErrorLogService.sanitizeForOpsLog(String.valueOf(value));
    }

    @FunctionalInterface
    private interface ToolSupplier {
        String get();
    }
}
