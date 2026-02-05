package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.HouseholdExpense;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillReminderResponse {
    
    private UUID id;
    private UUID householdId;
    private String name;
    private HouseholdExpense.ExpenseCategory category;
    private Integer amount;
    private Boolean isRecurring;
    private Integer dueDay;
    private Integer reminderDaysBefore;
    private Boolean isActive;
    
    // Next due date info
    private Integer daysUntilDue;
    private Boolean isPastDue;
    
    private UUID createdById;
    private String createdByName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
