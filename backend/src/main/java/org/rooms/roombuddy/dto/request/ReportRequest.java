package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.Report;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    
    @NotNull(message = "Entity type is required")
    private Report.EntityType entityType;
    
    @NotNull(message = "Entity ID is required")
    private UUID entityId;
    
    @NotNull(message = "Reason is required")
    private Report.ReportReason reason;
    
    @NotBlank(message = "Description is required")
    private String description;
}
