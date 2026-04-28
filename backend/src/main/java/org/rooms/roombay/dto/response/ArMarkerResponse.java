package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArMarkerResponse {
    private UUID id;
    private Double x;
    private Double y;
    private Double z;
    private String label;
    private String color;
    private LocalDateTime createdAt;
}
