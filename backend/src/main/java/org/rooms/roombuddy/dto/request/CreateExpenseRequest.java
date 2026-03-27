package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.rooms.roombay.entity.HouseholdExpense;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
public class CreateExpenseRequest {
    
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be positive")
    private Integer amount;
    
    @NotNull(message = "Category is required")
    private HouseholdExpense.ExpenseCategory category;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private String receiptUrl;
    
    private LocalDate expenseDate;
    
    @NotNull(message = "Split type is required")
    private HouseholdExpense.SplitType splitType;
    
    // For CUSTOM split type: userId -> amount
    private Map<UUID, Integer> customSplits;
    
    private Boolean isRecurring;
    
    private Integer recurringDay;
}
