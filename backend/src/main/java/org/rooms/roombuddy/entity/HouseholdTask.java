package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Household tasks and chores.
 */
@Entity
@Table(name = "household_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskCategory category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", length = 20)
    @Builder.Default
    private AssignmentType assignmentType = AssignmentType.VOLUNTARY;
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "due_time")
    private LocalTime dueTime;
    
    @Column(name = "is_recurring")
    @Builder.Default
    private Boolean isRecurring = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_pattern", length = 30)
    private RecurrencePattern recurrencePattern;
    
    @Column(name = "recurrence_day")
    private Integer recurrenceDay;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskPriority priority = TaskPriority.NORMAL;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum TaskCategory {
        CLEANING,
        SHOPPING,
        MAINTENANCE,
        COOKING,
        LAUNDRY,
        TRASH,
        OTHER
    }
    
    public enum AssignmentType {
        VOLUNTARY,  // Anyone can do it
        ROTATING,   // Rotates between members
        ASSIGNED    // Assigned to specific person
    }
    
    public enum RecurrencePattern {
        DAILY,
        WEEKLY,
        BIWEEKLY,
        MONTHLY
    }
    
    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        OVERDUE,
        CANCELLED
    }
    
    public enum TaskPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
    
    public void markCompleted(User user) {
        this.completedBy = user;
        this.completedAt = LocalDateTime.now();
        this.status = TaskStatus.COMPLETED;
    }
}
