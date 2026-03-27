package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.rooms.roombay.entity.ExpenseSettlement;

import java.util.UUID;

@Data
public class SettlementRequest {
    
    @NotNull(message = "Recipient is required")
    private UUID toUserId;
    
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be positive")
    private Integer amount;
    
    private String note;
    
    private ExpenseSettlement.SettlementMethod settlementMethod;
}
