package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomPreviewLayoutResponse {
    private UUID id;
    private UUID listingId;
    private String layoutName;
    private List<Map<String, Object>> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
