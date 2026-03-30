package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a settlement payment between household members.
 */
@Entity
@Table(name = "expense_settlements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSettlement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user", nullable = false)
    private User fromUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user", nullable = false)
    private User toUser;
    
    @Column(nullable = false)
    private Integer amount;
    
    @Column(length = 255)
    private String note;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_method", length = 30)
    private SettlementMethod settlementMethod;
    
    @Column(name = "settled_at", nullable = false)
    @Builder.Default
    private LocalDateTime settledAt = LocalDateTime.now();
    
    public enum SettlementMethod {
        CASH,
        MOBILE_MONEY,
        BANK_TRANSFER,
        OTHER
    }
}
