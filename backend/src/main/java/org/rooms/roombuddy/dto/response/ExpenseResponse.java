package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.HouseholdExpense;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    
    private UUID id;
    private UUID householdId;
    private Integer amount;
    private HouseholdExpense.ExpenseCategory category;
    private String description;
    private String receiptUrl;
    private LocalDate expenseDate;
    private HouseholdExpense.SplitType splitType;
    private Boolean isRecurring;
    private Integer recurringDay;
    private LocalDateTime createdAt;
    
    // Who paid
    private UUID paidById;
    private String paidByName;
    private String paidByPhoto;
    
    // Splits
    private List<SplitResponse> splits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitResponse {
        private UUID id;
        private UUID userId;
        private String userName;
        private String userPhoto;
        private Integer amount;
        private Boolean isPaid;
        private LocalDateTime paidAt;
    }
}
