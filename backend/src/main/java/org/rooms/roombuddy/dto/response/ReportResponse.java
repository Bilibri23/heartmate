package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.Report;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private UUID id;
    private UUID reporterId;
    private String reporterName;
    private String reporterEmail;
    private Report.EntityType reportedEntityType;
    private UUID reportedEntityId;
    private String reportedEntityName;
    private Report.ReportReason reason;
    private String description;
    private Report.ReportStatus status;
    private Report.ReportPriority priority;
    private UUID reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String resolutionNotes;
    private Report.ReportAction actionTaken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
