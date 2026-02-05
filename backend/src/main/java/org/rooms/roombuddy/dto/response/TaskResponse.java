package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.HouseholdTask;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    
    private UUID id;
    private UUID householdId;
    private String title;
    private String description;
    private HouseholdTask.TaskCategory category;
    private HouseholdTask.AssignmentType assignmentType;
    private HouseholdTask.TaskStatus status;
    private HouseholdTask.TaskPriority priority;
    
    private LocalDate dueDate;
    private LocalTime dueTime;
    private Boolean isOverdue;
    
    private Boolean isRecurring;
    private HouseholdTask.RecurrencePattern recurrencePattern;
    private Integer recurrenceDay;
    
    // Creator
    private UUID createdById;
    private String createdByName;
    
    // Assigned to
    private UUID assignedToId;
    private String assignedToName;
    private String assignedToPhoto;
    
    // Completion
    private UUID completedById;
    private String completedByName;
    private LocalDateTime completedAt;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
