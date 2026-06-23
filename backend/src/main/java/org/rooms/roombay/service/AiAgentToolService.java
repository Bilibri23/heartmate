package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.RoomApplicationRequest;
import org.rooms.roombay.dto.request.VisitRequest;
import org.rooms.roombay.dto.response.ListingResponse;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Allowlisted write tools for the LangGraph orchestrator. Every call is scoped to the
 * supplied userId/role (originally from Spring JWT) and re-validates business rules
 * in the domain services — the agent cannot bypass verification or ownership checks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAgentToolService {

    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "SAVE_LISTING_FAVORITE",
            "APPLY_TO_LISTING",
            "REQUEST_VISIT"
    );

    private static final String DEFAULT_APPLY_MESSAGE =
            "I am interested in this listing and would like to apply through RoomBay. "
                    + "Please review my profile and let me know the next steps.";

    private final ListingService listingService;
    private final ApplicationService applicationService;
    private final VisitService visitService;
    private final AnalyticsEventService analyticsEventService;

    public record ToolExecutionResult(String tool, boolean success, String message, String entityId) {}

    @Transactional
    public ToolExecutionResult execute(
            UUID userId,
            String role,
            String tool,
            Map<String, Object> params,
            String requestId) {
        String normalizedTool = tool == null ? "" : tool.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TOOLS.contains(normalizedTool)) {
            return new ToolExecutionResult(normalizedTool, false, "Tool not allowed: " + normalizedTool, null);
        }
        if (!"STUDENT".equalsIgnoreCase(role)) {
            return new ToolExecutionResult(normalizedTool, false, "Only tenants can run this action", null);
        }

        UUID listingId = parseUuid(params == null ? null : params.get("listingId"));
        if (listingId == null) {
            return new ToolExecutionResult(normalizedTool, false, "listingId is required", null);
        }

        try {
            return switch (normalizedTool) {
                case "SAVE_LISTING_FAVORITE" -> saveFavorite(userId, listingId, requestId);
                case "APPLY_TO_LISTING" -> applyToListing(userId, listingId, params, requestId);
                case "REQUEST_VISIT" -> requestVisit(userId, listingId, params, requestId);
                default -> new ToolExecutionResult(normalizedTool, false, "Unsupported tool", null);
            };
        } catch (BadRequestException | ResourceNotFoundException ex) {
            log.info("[AI][agent-tool] {} user={} listing={} failed: {}", normalizedTool, userId, listingId, ex.getMessage());
            return new ToolExecutionResult(normalizedTool, false, ex.getMessage(), null);
        } catch (Exception ex) {
            log.warn("[AI][agent-tool] {} user={} listing={} error: {}", normalizedTool, userId, listingId, ex.getMessage());
            return new ToolExecutionResult(normalizedTool, false, "Action could not be completed", null);
        }
    }

    private ToolExecutionResult saveFavorite(UUID userId, UUID listingId, String requestId) {
        ListingResponse listing = listingService.toggleFavorite(listingId, userId);
        boolean saved = Boolean.TRUE.equals(listing.getIsFavorite());
        analyticsEventService.emit("ai_agent_tool_executed", userId, "STUDENT", listingId,
                Map.of("tool", "SAVE_LISTING_FAVORITE", "success", true, "saved", saved, "requestId", safe(requestId)));
        String message = saved
                ? "Saved " + listing.getTitle() + " to your favorites."
                : "Removed " + listing.getTitle() + " from your favorites.";
        return new ToolExecutionResult("SAVE_LISTING_FAVORITE", true, message, listingId.toString());
    }

    private ToolExecutionResult applyToListing(
            UUID userId, UUID listingId, Map<String, Object> params, String requestId) {
        String message = stringParam(params, "message", DEFAULT_APPLY_MESSAGE);
        if (message.length() < 50) {
            message = DEFAULT_APPLY_MESSAGE;
        }
        LocalDate moveIn = parseDate(params == null ? null : params.get("moveInDate"));
        if (moveIn == null) {
            moveIn = LocalDate.now().plusMonths(1);
        }
        Integer leaseMonths = parseInteger(params == null ? null : params.get("leaseDurationMonths"));
        if (leaseMonths == null) {
            leaseMonths = 12;
        }

        var response = applicationService.createApplication(userId, RoomApplicationRequest.builder()
                .listingId(listingId)
                .message(message)
                .moveInDate(moveIn)
                .leaseDurationMonths(leaseMonths)
                .build());

        analyticsEventService.emit("ai_agent_tool_executed", userId, "STUDENT", listingId,
                Map.of("tool", "APPLY_TO_LISTING", "success", true,
                        "applicationId", response.getId().toString(), "requestId", safe(requestId)));
        return new ToolExecutionResult(
                "APPLY_TO_LISTING",
                true,
                "Application submitted for " + response.getListingTitle() + ".",
                response.getId().toString());
    }

    private ToolExecutionResult requestVisit(
            UUID userId, UUID listingId, Map<String, Object> params, String requestId) {
        LocalDateTime when = parseDateTime(params == null ? null : params.get("requestedDatetime"));
        if (when == null) {
            when = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        }
        String note = stringParam(params, "message", "Visit requested via RoomBay AI assistant.");

        var response = visitService.requestVisit(userId, VisitRequest.builder()
                .listingId(listingId)
                .requestedDatetime(when)
                .message(note)
                .build());

        analyticsEventService.emit("ai_agent_tool_executed", userId, "STUDENT", listingId,
                Map.of("tool", "REQUEST_VISIT", "success", true,
                        "visitId", response.getId().toString(), "requestId", safe(requestId)));
        return new ToolExecutionResult(
                "REQUEST_VISIT",
                true,
                "Visit requested for " + response.getListingTitle() + ".",
                response.getId().toString());
    }

    private static UUID parseUuid(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(raw).trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Integer parseInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(Object raw) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(String.valueOf(raw).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseDateTime(Object raw) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(raw).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String stringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || params.get(key) == null) {
            return defaultValue;
        }
        String value = String.valueOf(params.get(key)).trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private static String safe(String requestId) {
        return requestId == null ? "" : requestId;
    }
}
