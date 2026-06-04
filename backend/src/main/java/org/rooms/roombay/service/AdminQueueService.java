package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import org.rooms.roombay.dto.request.AdminQueueActionRequest;
import org.rooms.roombay.dto.request.ListingApprovalRequest;
import org.rooms.roombay.dto.request.VerificationApprovalRequest;
import org.rooms.roombay.dto.response.AdminQueueItemResponse;
import org.rooms.roombay.entity.LandlordVerification;
import org.rooms.roombay.entity.Payment;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.Report;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.StudentVerification;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.PaymentRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.ReportRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminQueueService {

    private static final int MAX_PER_SOURCE = 25;

    private final StudentVerificationRepository studentVerificationRepository;
    private final LandlordVerificationRepository landlordVerificationRepository;
    private final PropertyListingRepository listingRepository;
    private final PaymentRepository paymentRepository;
    private final ReportRepository reportRepository;
    private final RoomApplicationRepository applicationRepository;
    private final AdminOpsService adminOpsService;
    private final VerificationService verificationService;
    private final ListingService listingService;
    private final PaymentService paymentService;
    private final ReportService reportService;

    public List<AdminQueueItemResponse> list(String queueType, String priority, String status, int limit) {
        List<AdminQueueItemResponse> items = new ArrayList<>();
        items.addAll(tenantVerificationItems());
        items.addAll(landlordVerificationItems());
        items.addAll(listingReviewItems());
        items.addAll(paymentProofItems());
        items.addAll(reportItems());
        items.addAll(staleApplicationItems());
        items.addAll(aiNoAnswerItems());
        items.addAll(criticalAlertItems());

        return items.stream()
                .filter(item -> matches(queueType, item.getQueueType()))
                .filter(item -> matches(priority, item.getPriority()))
                .filter(item -> matches(status, item.getStatus()))
                .sorted(Comparator
                        .comparingInt((AdminQueueItemResponse item) -> priorityRank(item.getPriority()))
                        .thenComparing(AdminQueueItemResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(Math.min(Math.max(limit, 1), 200))
                .toList();
    }

    public Map<String, Object> applyAction(String queueType, UUID targetId, UUID adminId, AdminQueueActionRequest request) {
        String type = requireText(queueType, "queueType").toUpperCase();
        String action = requireText(request != null ? request.getAction() : null, "action").toUpperCase();
        String note = request != null ? request.getNote() : null;
        requireNoteForSensitiveAction(action, note);

        Object result = switch (type) {
            case "TENANT_VERIFICATION" -> verificationService.approveOrRejectVerification(targetId, adminId,
                    VerificationApprovalRequest.builder()
                            .status(actionToVerificationStatus(action))
                            .rejectionReason(note)
                            .build());
            case "LISTING_REVIEW" -> listingService.approveOrRejectListing(targetId, adminId,
                    ListingApprovalRequest.builder()
                            .status(actionToListingStatus(action))
                            .rejectionReason(note)
                            .build());
            case "PAYMENT_PROOF" -> switch (action) {
                case "VERIFY" -> paymentService.verifyPayment(targetId, adminId, note);
                case "REJECT" -> paymentService.rejectPayment(targetId, adminId, note);
                default -> throw new BadRequestException("Unsupported payment queue action: " + action);
            };
            case "REPORT" -> switch (action) {
                case "RESOLVE" -> reportService.resolveReport(targetId, adminId, note, Report.ReportAction.NO_ACTION);
                case "DISMISS" -> reportService.dismissReport(targetId, adminId, note);
                default -> throw new BadRequestException("Unsupported report queue action: " + action);
            };
            default -> throw new BadRequestException("Queue action is not available for " + type);
        };

        return Map.of("status", "ok", "queueType", type, "targetId", targetId, "action", action, "result", result);
    }

    private List<AdminQueueItemResponse> tenantVerificationItems() {
        return studentVerificationRepository
                .findByStatusOrderByCreatedAtAsc(StudentVerification.Status.PENDING, PageRequest.of(0, MAX_PER_SOURCE))
                .stream()
                .map(v -> item("TENANT_VERIFICATION", "MEDIUM", "Student verification pending",
                        userLabel(v.getUser() != null ? v.getUser().getEmail() : null),
                        "TENANT_VERIFICATION", v.getId(), v.getUser() != null ? v.getUser().getId() : null,
                        v.getStatus().name(), v.getCreatedAt(), "Review submitted tenant verification documents.",
                        List.of("APPROVE", "REJECT"), "/admin/verifications"))
                .toList();
    }

    private List<AdminQueueItemResponse> landlordVerificationItems() {
        return landlordVerificationRepository.findAllPendingVerifications(PageRequest.of(0, MAX_PER_SOURCE))
                .getContent()
                .stream()
                .map(v -> item("LANDLORD_VERIFICATION", "HIGH", "Landlord verification pending",
                        pendingLandlordVerificationDescription(v),
                        "LANDLORD_VERIFICATION", v.getId(), v.getUser() != null ? v.getUser().getId() : null,
                        "PENDING", v.getCreatedAt(), "Open landlord verification and review each submitted section.",
                        List.of(), "/admin/verifications"))
                .toList();
    }

    private List<AdminQueueItemResponse> listingReviewItems() {
        return listingRepository.findPendingListings(PageRequest.of(0, MAX_PER_SOURCE)).stream()
                .map(l -> item("LISTING_REVIEW", "HIGH", valueOr(l.getTitle(), "Listing review pending"),
                        valueOr(l.getCity(), "Unknown city") + " / " + valueOr(l.getNeighborhood(), "unknown neighborhood"),
                        "LISTING", l.getId(), l.getLandlord() != null ? l.getLandlord().getId() : null,
                        l.getStatus().name(), l.getCreatedAt(), "Approve if accurate and safe, otherwise reject with a clear reason.",
                        List.of("APPROVE", "REJECT"), "/admin/listings"))
                .toList();
    }

    private List<AdminQueueItemResponse> paymentProofItems() {
        return paymentRepository.findPendingVerification(PageRequest.of(0, MAX_PER_SOURCE, Sort.by("createdAt").ascending()))
                .getContent()
                .stream()
                .map(p -> item("PAYMENT_PROOF", "HIGH", "Payment proof awaiting review",
                        p.getReferenceCode() + " - " + p.getAmount() + " XAF",
                        "PAYMENT", p.getId(), p.getPayer() != null ? p.getPayer().getId() : null,
                        p.getStatus().name(), p.getCreatedAt(), "Verify receipt details and duplicate-risk warnings before approving.",
                        List.of("VERIFY", "REJECT"), "/admin/payments"))
                .toList();
    }

    private List<AdminQueueItemResponse> reportItems() {
        return reportRepository.findByStatusOrderByCreatedAtDesc(
                        Report.ReportStatus.PENDING,
                        PageRequest.of(0, MAX_PER_SOURCE, Sort.by("createdAt").ascending()))
                .getContent()
                .stream()
                .map(r -> item("REPORT", reportPriority(r), r.getReason().name() + " report",
                        r.getReportedEntityType().name() + " " + r.getReportedEntityId(),
                        "REPORT", r.getId(), r.getReporterId(),
                        r.getStatus().name(), r.getCreatedAt(), "Review report details and choose a moderation outcome.",
                        List.of("RESOLVE", "DISMISS"), "/admin/reports"))
                .toList();
    }

    private List<AdminQueueItemResponse> staleApplicationItems() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        return applicationRepository.findStaleActiveApplications(cutoff, PageRequest.of(0, MAX_PER_SOURCE)).stream()
                .map(a -> item("STALE_APPLICATION", "INFO", "Application needs landlord response",
                        a.getListing() != null ? valueOr(a.getListing().getTitle(), "Listing") : "Listing",
                        "APPLICATION", a.getId(), a.getStudent() != null ? a.getStudent().getId() : null,
                        a.getStatus().name(), a.getCreatedAt(), "Nudge landlord response or inspect the application thread.",
                        List.of(), "/admin/ops"))
                .toList();
    }

    private List<AdminQueueItemResponse> aiNoAnswerItems() {
        return adminOpsService.topNoAnswerQuestions("7d", 10).stream()
                .map(row -> {
                    String question = String.valueOf(row.getOrDefault("question", "AI question needs documentation"));
                    String role = String.valueOf(row.getOrDefault("role", "UNKNOWN"));
                    Object lastSeen = row.get("lastSeen");
                    LocalDateTime createdAt = lastSeen instanceof LocalDateTime time ? time : LocalDateTime.now();
                    return item("AI_UNANSWERED", "MEDIUM", "AI missing knowledge",
                            role + ": " + truncate(question, 120),
                            "AI_DOCS", null, null, "OPEN", createdAt,
                            "Add or improve role-safe docs, then run admin AI ingest.",
                            List.of(), "/admin/settings");
                })
                .toList();
    }

    private List<AdminQueueItemResponse> criticalAlertItems() {
        return adminOpsService.alerts().stream()
                .filter(alert -> !"INFO".equalsIgnoreCase(String.valueOf(alert.get("severity"))))
                .map(alert -> item("CRITICAL_ALERT", String.valueOf(alert.get("severity")),
                        String.valueOf(alert.get("title")),
                        String.valueOf(alert.get("message")),
                        "ALERT", null, null, String.valueOf(alert.getOrDefault("code", "OPEN")),
                        LocalDateTime.now(), String.valueOf(alert.get("suggestedAction")),
                        List.of(), "/admin/ops"))
                .toList();
    }

    private static AdminQueueItemResponse item(
            String queueType,
            String priority,
            String title,
            String description,
            String targetType,
            UUID targetId,
            UUID relatedUserId,
            String status,
            LocalDateTime createdAt,
            String suggestedAction,
            List<String> availableActions,
            String href
    ) {
        String id = queueType + ":" + (targetId != null ? targetId : title.hashCode());
        return AdminQueueItemResponse.builder()
                .id(id)
                .queueType(queueType)
                .priority(priority)
                .title(title)
                .description(truncate(description, 240))
                .targetType(targetType)
                .targetId(targetId)
                .relatedUserId(relatedUserId)
                .status(status)
                .createdAt(createdAt)
                .age(age(createdAt))
                .suggestedAction(suggestedAction)
                .availableActions(availableActions)
                .href(href)
                .build();
    }

    private static boolean matches(String filter, String value) {
        return !StringUtils.hasText(filter) || "all".equalsIgnoreCase(filter) || filter.equalsIgnoreCase(value);
    }

    private static String actionToVerificationStatus(String action) {
        return switch (action) {
            case "APPROVE" -> "VERIFIED";
            case "REJECT" -> "REJECTED";
            default -> throw new BadRequestException("Unsupported verification queue action: " + action);
        };
    }

    private static String actionToListingStatus(String action) {
        return switch (action) {
            case "APPROVE" -> "ACTIVE";
            case "REJECT" -> "REJECTED";
            default -> throw new BadRequestException("Unsupported listing queue action: " + action);
        };
    }

    private static void requireNoteForSensitiveAction(String action, String note) {
        if ((action.equals("REJECT") || action.equals("DISMISS") || action.equals("RESOLVE"))
                && !StringUtils.hasText(note)) {
            throw new BadRequestException("A note is required for " + action.toLowerCase() + " actions");
        }
    }

    private static String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(field + " is required");
        }
        return value.trim();
    }

    private static int priorityRank(String priority) {
        return switch (String.valueOf(priority).toUpperCase()) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "WARNING" -> 2;
            case "MEDIUM" -> 3;
            default -> 4;
        };
    }

    private static String reportPriority(Report report) {
        if (report.getPriority() == Report.ReportPriority.CRITICAL) {
            return "CRITICAL";
        }
        if (report.getPriority() == Report.ReportPriority.HIGH) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private static String pendingLandlordVerificationDescription(LandlordVerification verification) {
        List<String> pending = new ArrayList<>();
        if (verification.getIdentityStatus() == LandlordVerification.VerificationStatus.PENDING) pending.add("identity");
        if (verification.getBusinessStatus() == LandlordVerification.VerificationStatus.PENDING) pending.add("business");
        if (verification.getPropertyStatus() == LandlordVerification.VerificationStatus.PENDING) pending.add("property");
        return "Pending sections: " + (pending.isEmpty() ? "review" : String.join(", ", pending));
    }

    private static String userLabel(String email) {
        return StringUtils.hasText(email) ? email : "User verification awaiting review";
    }

    private static String valueOr(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }

    private static String age(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "-";
        }
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long days = duration.toDays();
        if (days > 0) {
            return days + "d";
        }
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + "h";
        }
        return Math.max(duration.toMinutes(), 0) + "m";
    }
}
