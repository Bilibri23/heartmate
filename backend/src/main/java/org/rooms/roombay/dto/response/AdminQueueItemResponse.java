package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminQueueItemResponse {
    private String id;
    private String queueType;
    private String priority;
    private String title;
    private String description;
    private String targetType;
    private UUID targetId;
    private UUID relatedUserId;
    private String status;
    private LocalDateTime createdAt;
    private String age;
    private String suggestedAction;
    private List<String> availableActions;
    private String href;
}
