package org.rooms.roombay.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rooms.roombay.dto.request.RoomPreviewLayoutRequest;
import org.rooms.roombay.dto.response.RoomPreviewLayoutResponse;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.AnalyticsEventService;
import org.rooms.roombay.service.RoomPreviewLayoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RoomPreviewLayoutController {

    private final RoomPreviewLayoutService roomPreviewLayoutService;
    private final AnalyticsEventService analyticsEventService;

    @GetMapping("/api/listings/{listingId}/room-preview-layouts")
    public ResponseEntity<List<RoomPreviewLayoutResponse>> getLayouts(@PathVariable UUID listingId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(roomPreviewLayoutService.getLayouts(listingId, userId));
    }

    @PostMapping("/api/listings/{listingId}/room-preview-layouts")
    public ResponseEntity<RoomPreviewLayoutResponse> saveLayout(
            @PathVariable UUID listingId,
            @Valid @RequestBody RoomPreviewLayoutRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();
        return ResponseEntity.ok(roomPreviewLayoutService.saveLayout(listingId, userId, role, request));
    }

    @DeleteMapping("/api/room-preview-layouts/{layoutId}")
    public ResponseEntity<Void> deleteLayout(@PathVariable UUID layoutId) {
        roomPreviewLayoutService.deleteLayout(layoutId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/listings/{listingId}/room-preview/opened")
    public ResponseEntity<Void> trackOpened(@PathVariable UUID listingId) {
        analyticsEventService.emit(
                "room_preview_opened",
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUserRole(),
                listingId,
                Map.of("source", "listing_detail"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/listings/{listingId}/room-preview/events")
    public ResponseEntity<Void> trackEvent(
            @PathVariable UUID listingId,
            @RequestBody Map<String, Object> body) {
        String eventType = String.valueOf(body.getOrDefault("eventType", ""));
        if (!List.of("room_preview_preset_selected", "room_preview_item_added").contains(eventType)) {
            return ResponseEntity.badRequest().build();
        }
        analyticsEventService.emit(
                eventType,
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUserRole(),
                listingId,
                Map.of("source", "room_preview_studio"));
        return ResponseEntity.ok().build();
    }
}
