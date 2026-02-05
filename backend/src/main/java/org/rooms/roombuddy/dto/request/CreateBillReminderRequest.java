package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.rooms.roombuddy.entity.HouseholdExpense;

@Data
public class CreateBillReminderRequest {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotNull(message = "Category is required")
    private HouseholdExpense.ExpenseCategory category;
    
    private Integer amount; // Expected amount
    
    private Boolean isRecurring;
    
    @Min(1)
    @Max(31)
    private Integer dueDay; // Day of month
    
    @Min(1)
    @Max(14)
    private Integer reminderDaysBefore;
}
