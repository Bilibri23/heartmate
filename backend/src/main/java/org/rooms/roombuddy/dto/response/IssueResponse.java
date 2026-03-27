package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.HouseholdIssue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponse {
    
    private UUID id;
    private UUID householdId;
    private String title;
    private String description;
    private HouseholdIssue.IssueCategory category;
    private HouseholdIssue.IssueSeverity severity;
    private HouseholdIssue.IssueStatus status;
    
    // Reporter (may be hidden if anonymous)
    private UUID reportedById;
    private String reportedByName;
    private Boolean isAnonymous;
    
    // Reported against (optional)
    private UUID reportedAgainstId;
    private String reportedAgainstName;
    
    // Resolution
    private String resolution;
    private LocalDateTime resolvedAt;
    private UUID resolvedById;
    private String resolvedByName;
    
    // Escalation
    private Boolean isEscalated;
    private LocalDateTime escalatedAt;
    private String escalatedTo;
    
    // Comments
    private List<CommentResponse> comments;
    private Integer commentCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentResponse {
        private UUID id;
        private UUID userId;
        private String userName;
        private String userPhoto;
        private String content;
        private Boolean isResolutionProposal;
        private LocalDateTime createdAt;
    }
}
