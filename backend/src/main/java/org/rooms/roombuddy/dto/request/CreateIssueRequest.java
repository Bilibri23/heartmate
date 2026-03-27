package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.rooms.roombay.entity.HouseholdIssue;

import java.util.UUID;

@Data
public class CreateIssueRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Category is required")
    private HouseholdIssue.IssueCategory category;
    
    private HouseholdIssue.IssueSeverity severity;
    
    private UUID reportedAgainst; // Optional
    
    private Boolean isAnonymous;
}
