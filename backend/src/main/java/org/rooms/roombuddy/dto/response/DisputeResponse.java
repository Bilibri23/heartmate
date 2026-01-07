package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.Dispute;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponse {
    
    private UUID id;
    private String referenceCode;
    
    // Lease info
    private UUID leaseId;
    private String leaseReferenceCode;
    
    // Parties
    private UUID openedById;
    private String openedByName;
    private UUID againstUserId;
    private String againstUserName;
    
    // Dispute details
    private Dispute.DisputeCategory category;
    private String title;
    private String description;
    private List<String> evidenceUrls;
    
    // Status
    private Dispute.DisputeStatus status;
    private Dispute.DisputePriority priority;
    
    // Assignment
    private UUID assignedToId;
    private String assignedToName;
    private LocalDateTime assignedAt;
    
    // Resolution
    private Dispute.DisputeOutcome outcome;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;
    private String resolvedByName;
    
    // Refund
    private Integer refundAmount;
    private Boolean refundApproved;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
