package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Derived, read-only progress timeline for an application, stitched from application,
 * visit, lease, and payment state. No schema of its own.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationTimelineResponse {

    private UUID applicationId;
    private List<Step> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        private String key;
        private String label;
        private String status; // DONE | CURRENT | PENDING
        private LocalDateTime at;
    }
}
