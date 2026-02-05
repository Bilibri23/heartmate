package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSummaryResponse {
    
    private UUID householdId;
    private UUID userId;
    
    // Total amounts
    private Long totalPaidByMe;
    private Long totalOwedByMe;
    private Long totalOwedToMe;
    private Long netBalance; // positive = others owe me, negative = I owe others
    
    // Per-member balances
    private List<MemberBalance> memberBalances;
    
    // Simplified debts (who owes whom)
    private List<DebtSummary> debts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberBalance {
        private UUID userId;
        private String userName;
        private String userPhoto;
        private Long amountOwed; // What they owe me (positive) or I owe them (negative)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebtSummary {
        private UUID fromUserId;
        private String fromUserName;
        private UUID toUserId;
        private String toUserName;
        private Long amount;
    }
}
