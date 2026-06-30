package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.ApplicationReviewRequest;
import org.rooms.roombay.dto.request.LandlordVerificationApprovalRequest;
import org.rooms.roombay.dto.request.ListingApprovalRequest;
import org.rooms.roombay.dto.request.VisitUpdateRequest;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.rooms.roombay.entity.Report;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.Visit;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Privileged (landlord/admin) write tools the AI assistant can run <em>only after explicit user
 * confirmation</em> (the confirm-button gate). Every call is scoped to the supplied userId/role and
 * re-validates ownership/authority in the domain services — the assistant cannot bypass the checks
 * a human would hit on the real screen. User-account suspension is intentionally excluded.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiPrivilegedActionService {

    // Landlord tools
    public static final String ACCEPT_APPLICATION = "ACCEPT_APPLICATION";
    public static final String REJECT_APPLICATION = "REJECT_APPLICATION";
    public static final String SHORTLIST_APPLICATION = "SHORTLIST_APPLICATION";
    public static final String ACCEPT_VISIT = "ACCEPT_VISIT";
    public static final String CANCEL_VISIT = "CANCEL_VISIT";
    public static final String COMPLETE_VISIT = "COMPLETE_VISIT";
    // Admin tools
    public static final String APPROVE_LISTING = "APPROVE_LISTING";
    public static final String REJECT_LISTING = "REJECT_LISTING";
    public static final String APPROVE_VERIFICATION = "APPROVE_VERIFICATION";
    public static final String REJECT_VERIFICATION = "REJECT_VERIFICATION";
    public static final String VERIFY_PAYMENT = "VERIFY_PAYMENT";
    public static final String RESOLVE_REPORT = "RESOLVE_REPORT";

    private static final Set<String> LANDLORD_TOOLS = Set.of(
            ACCEPT_APPLICATION, REJECT_APPLICATION, SHORTLIST_APPLICATION,
            ACCEPT_VISIT, CANCEL_VISIT, COMPLETE_VISIT);
    private static final Set<String> ADMIN_TOOLS = Set.of(
            APPROVE_LISTING, REJECT_LISTING, APPROVE_VERIFICATION, REJECT_VERIFICATION,
            VERIFY_PAYMENT, RESOLVE_REPORT);

    private final ApplicationService applicationService;
    private final VisitService visitService;
    private final ListingService listingService;
    private final LandlordVerificationService landlordVerificationService;
    private final PaymentService paymentService;
    private final ReportService reportService;
    private final AnalyticsEventService analyticsEventService;

    public boolean isLandlordTool(String tool) {
        return LANDLORD_TOOLS.contains(normalize(tool));
    }

    public boolean isAdminTool(String tool) {
        return ADMIN_TOOLS.contains(normalize(tool));
    }

    @Transactional
    public AiChatResponse.ToolExecution execute(UUID userId, String role, String tool, Map<String, Object> params) {
        String normalizedTool = normalize(tool);
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);

        if (isLandlordTool(normalizedTool)) {
            if (!"LANDLORD".equals(normalizedRole)) {
                return fail(normalizedTool, "Only landlords can run this action.");
            }
        } else if (isAdminTool(normalizedTool)) {
            if (!"ADMIN".equals(normalizedRole)) {
                return fail(normalizedTool, "Only administrators can run this action.");
            }
        } else {
            return fail(normalizedTool, "Unknown or unsupported action: " + normalizedTool);
        }

        UUID targetId = parseUuid(params == null ? null : params.get("targetId"));
        if (targetId == null) {
            return fail(normalizedTool, "A target id is required to run this action.");
        }
        String reason = stringParam(params, "reason");
        String type = stringParam(params, "verificationType");

        try {
            AiChatResponse.ToolExecution result = switch (normalizedTool) {
                case ACCEPT_APPLICATION -> reviewApplication(userId, targetId, RoomApplication.Status.ACCEPTED, reason);
                case REJECT_APPLICATION -> reviewApplication(userId, targetId, RoomApplication.Status.REJECTED, reason);
                case SHORTLIST_APPLICATION -> reviewApplication(userId, targetId, RoomApplication.Status.SHORTLISTED, reason);
                case ACCEPT_VISIT -> updateVisit(userId, targetId, Visit.Status.ACCEPTED, reason);
                case CANCEL_VISIT -> updateVisit(userId, targetId, Visit.Status.CANCELLED, reason);
                case COMPLETE_VISIT -> updateVisit(userId, targetId, Visit.Status.COMPLETED, reason);
                case APPROVE_LISTING -> approveListing(userId, targetId, true, reason);
                case REJECT_LISTING -> approveListing(userId, targetId, false, reason);
                case APPROVE_VERIFICATION -> reviewVerification(userId, targetId, true, type, reason);
                case REJECT_VERIFICATION -> reviewVerification(userId, targetId, false, type, reason);
                case VERIFY_PAYMENT -> verifyPayment(userId, targetId, reason);
                case RESOLVE_REPORT -> resolveReport(userId, targetId, reason);
                default -> fail(normalizedTool, "Unsupported action.");
            };
            analyticsEventService.emit("ai_privileged_action_executed", userId, normalizedRole, targetId,
                    Map.of("tool", normalizedTool, "success", Boolean.TRUE.equals(result.getSuccess())));
            log.info("[AI][priv-action] {} user={} target={} success={}", normalizedTool, userId, targetId, result.getSuccess());
            return result;
        } catch (BadRequestException | ResourceNotFoundException ex) {
            log.info("[AI][priv-action] {} user={} target={} rejected: {}", normalizedTool, userId, targetId, ex.getMessage());
            return fail(normalizedTool, ex.getMessage());
        } catch (Exception ex) {
            log.warn("[AI][priv-action] {} user={} target={} error: {}", normalizedTool, userId, targetId, ex.getMessage());
            return fail(normalizedTool, "Action could not be completed.");
        }
    }

    private AiChatResponse.ToolExecution reviewApplication(UUID landlordId, UUID applicationId,
                                                           RoomApplication.Status status, String reason) {
        ApplicationReviewRequest request = ApplicationReviewRequest.builder()
                .status(status)
                .response(reason)
                .rejectionReason(status == RoomApplication.Status.REJECTED
                        ? (reason == null || reason.isBlank() ? "Reviewed via RoomBay AI assistant." : reason) : null)
                .build();
        applicationService.reviewApplication(applicationId, landlordId, request);
        String verb = switch (status) {
            case ACCEPTED -> "Accepted";
            case REJECTED -> "Rejected";
            case SHORTLISTED -> "Shortlisted";
            default -> "Updated";
        };
        return ok(status == RoomApplication.Status.REJECTED ? REJECT_APPLICATION
                        : status == RoomApplication.Status.SHORTLISTED ? SHORTLIST_APPLICATION : ACCEPT_APPLICATION,
                verb + " application.", applicationId);
    }

    private AiChatResponse.ToolExecution updateVisit(UUID landlordId, UUID visitId, Visit.Status status, String reason) {
        VisitUpdateRequest request = VisitUpdateRequest.builder()
                .status(status)
                .reason(reason)
                .build();
        visitService.updateVisit(visitId, landlordId, request);
        String verb = switch (status) {
            case ACCEPTED -> "Accepted";
            case CANCELLED -> "Cancelled";
            case COMPLETED -> "Marked completed";
            default -> "Updated";
        };
        String tool = switch (status) {
            case CANCELLED -> CANCEL_VISIT;
            case COMPLETED -> COMPLETE_VISIT;
            default -> ACCEPT_VISIT;
        };
        return ok(tool, verb + " visit.", visitId);
    }

    private AiChatResponse.ToolExecution approveListing(UUID adminId, UUID listingId, boolean approve, String reason) {
        ListingApprovalRequest request = ListingApprovalRequest.builder()
                .status(approve ? "ACTIVE" : "REJECTED")
                .rejectionReason(approve ? null
                        : (reason == null || reason.isBlank() ? "Rejected via RoomBay AI assistant." : reason))
                .build();
        listingService.approveOrRejectListing(listingId, adminId, request);
        return ok(approve ? APPROVE_LISTING : REJECT_LISTING,
                (approve ? "Approved" : "Rejected") + " listing.", listingId);
    }

    private AiChatResponse.ToolExecution reviewVerification(UUID adminId, UUID verificationId, boolean approve,
                                                            String type, String reason) {
        String verificationType = normalizeVerificationType(type);
        LandlordVerificationApprovalRequest request = LandlordVerificationApprovalRequest.builder()
                .verificationType(verificationType)
                .status(approve ? "VERIFIED" : "REJECTED")
                .rejectionReason(approve ? null
                        : (reason == null || reason.isBlank() ? "Rejected via RoomBay AI assistant." : reason))
                .build();
        landlordVerificationService.approveOrRejectVerification(verificationId, adminId, request);
        return ok(approve ? APPROVE_VERIFICATION : REJECT_VERIFICATION,
                (approve ? "Approved" : "Rejected") + " " + verificationType.toLowerCase(Locale.ROOT)
                        + " verification.", verificationId);
    }

    private AiChatResponse.ToolExecution verifyPayment(UUID adminId, UUID paymentId, String reason) {
        paymentService.verifyPayment(paymentId, adminId,
                reason == null || reason.isBlank() ? "Verified via RoomBay AI assistant." : reason);
        return ok(VERIFY_PAYMENT, "Payment verified.", paymentId);
    }

    private AiChatResponse.ToolExecution resolveReport(UUID adminId, UUID reportId, String reason) {
        reportService.resolveReport(reportId, adminId,
                reason == null || reason.isBlank() ? "Resolved via RoomBay AI assistant." : reason,
                Report.ReportAction.NO_ACTION);
        return ok(RESOLVE_REPORT, "Report resolved (no further action).", reportId);
    }

    private static String normalizeVerificationType(String type) {
        if (type == null) {
            return "IDENTITY";
        }
        String t = type.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case "BUSINESS", "PROPERTY", "IDENTITY" -> t;
            default -> "IDENTITY";
        };
    }

    private AiChatResponse.ToolExecution ok(String tool, String message, UUID entityId) {
        return AiChatResponse.ToolExecution.builder()
                .tool(tool).success(true).message(message)
                .entityId(entityId == null ? null : entityId.toString()).build();
    }

    private AiChatResponse.ToolExecution fail(String tool, String message) {
        return AiChatResponse.ToolExecution.builder()
                .tool(tool).success(false).message(message).build();
    }

    private static String normalize(String tool) {
        return tool == null ? "" : tool.trim().toUpperCase(Locale.ROOT);
    }

    private static String stringParam(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        String value = String.valueOf(params.get(key)).trim();
        return value.isEmpty() ? null : value;
    }

    private static UUID parseUuid(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
