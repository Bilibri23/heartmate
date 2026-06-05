package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomPreviewLayoutRequest {
    private UUID id;

    @NotBlank(message = "Layout name is required")
    @Size(max = 120, message = "Layout name must be 120 characters or less")
    private String layoutName;

    @NotNull(message = "Layout items are required")
    private List<Map<String, Object>> items;
}
