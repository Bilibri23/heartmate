package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Recurring bill reminders for household expenses.
 */
@Entity
@Table(name = "bill_reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillReminder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HouseholdExpense.ExpenseCategory category;
    
    @Column
    private Integer amount; // Expected amount (optional)
    
    @Column(name = "is_recurring")
    @Builder.Default
    private Boolean isRecurring = true;
    
    @Column(name = "due_day")
    private Integer dueDay; // Day of month (1-31)
    
    @Column(name = "reminder_days_before")
    @Builder.Default
    private Integer reminderDaysBefore = 3;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
