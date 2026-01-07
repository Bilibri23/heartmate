package org.rooms.roombuddy.dto.response;

import lombok.Builder;
import lombok.Data;
import org.rooms.roombuddy.entity.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private Notification.NotificationType type;
    private String title;
    private String message;
    private UUID referenceId;
    private String referenceType;
    private String actionUrl;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
