package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import org.rooms.roombay.dto.request.RoomPreviewLayoutRequest;
import org.rooms.roombay.dto.response.RoomPreviewLayoutResponse;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.RoomPreviewLayout;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.RoomPreviewLayoutRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomPreviewLayoutService {

    private final RoomPreviewLayoutRepository layoutRepository;
    private final PropertyListingRepository listingRepository;
    private final UserRepository userRepository;
    private final AnalyticsEventService analyticsEventService;

    public List<RoomPreviewLayoutResponse> getLayouts(UUID listingId, UUID userId) {
        ensureListingExists(listingId);
        return layoutRepository.findByUserIdAndListingIdOrderByUpdatedAtDesc(userId, listingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RoomPreviewLayoutResponse saveLayout(UUID listingId, UUID userId, String role, RoomPreviewLayoutRequest request) {
        PropertyListing listing = ensureListingExists(listingId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        RoomPreviewLayout layout = request.getId() == null
                ? RoomPreviewLayout.builder().user(user).listing(listing).build()
                : layoutRepository.findById(request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room preview layout not found"));

        if (layout.getId() != null && !layout.getUser().getId().equals(userId)) {
            throw new BadRequestException("Only the layout owner can update it");
        }
        if (layout.getId() != null && !layout.getListing().getId().equals(listingId)) {
            throw new BadRequestException("Layout does not belong to this listing");
        }

        layout.setLayoutName(request.getLayoutName().trim());
        layout.setItemsJson(sanitizeItems(request.getItems()));
        RoomPreviewLayout saved = layoutRepository.save(layout);
        analyticsEventService.emit("room_preview_saved", userId, role, listingId, Map.of("layoutName", saved.getLayoutName()));
        return toResponse(saved);
    }

    public void deleteLayout(UUID layoutId, UUID userId) {
        RoomPreviewLayout layout = layoutRepository.findById(layoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Room preview layout not found"));
        if (!layout.getUser().getId().equals(userId)) {
            throw new BadRequestException("Only the layout owner can delete it");
        }
        layoutRepository.delete(layout);
    }

    private PropertyListing ensureListingExists(UUID listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
    }

    private static List<Map<String, Object>> sanitizeItems(List<Map<String, Object>> items) {
        if (items == null) {
            return List.of();
        }
        if (items.size() > 40) {
            throw new BadRequestException("Room Preview supports up to 40 furniture items");
        }
        return items.stream().map(item -> Map.<String, Object>of(
                "type", String.valueOf(item.getOrDefault("type", "ITEM")).trim().toUpperCase(),
                "x", clampNumber(item.get("x"), 0, 1),
                "y", clampNumber(item.get("y"), 0, 1),
                "width", clampNumber(item.get("width"), 0.04, 1),
                "height", clampNumber(item.get("height"), 0.04, 1),
                "rotation", clampNumber(item.get("rotation"), -180, 180)
        )).toList();
    }

    private static double clampNumber(Object raw, double min, double max) {
        double value;
        try {
            value = raw instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(raw));
        } catch (Exception ignored) {
            value = min;
        }
        if (!Double.isFinite(value)) {
            value = min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private RoomPreviewLayoutResponse toResponse(RoomPreviewLayout layout) {
        return RoomPreviewLayoutResponse.builder()
                .id(layout.getId())
                .listingId(layout.getListing().getId())
                .layoutName(layout.getLayoutName())
                .items(layout.getItemsJson() != null ? layout.getItemsJson() : List.of())
                .createdAt(layout.getCreatedAt())
                .updatedAt(layout.getUpdatedAt())
                .build();
    }
}
