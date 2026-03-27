package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.rooms.roombay.entity.HouseholdTask;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class CreateTaskRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;
    
    private String description;
    
    @NotNull(message = "Category is required")
    private HouseholdTask.TaskCategory category;
    
    private UUID assignedTo;
    
    private HouseholdTask.AssignmentType assignmentType;
    
    private LocalDate dueDate;
    
    private LocalTime dueTime;
    
    private Boolean isRecurring;
    
    private HouseholdTask.RecurrencePattern recurrencePattern;
    
    private Integer recurrenceDay;
    
    private HouseholdTask.TaskPriority priority;
}
