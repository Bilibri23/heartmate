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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a shared expense that needs to be split among household members.
 */
@Entity
@Table(name = "household_expenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdExpense {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;
    
    @Column(nullable = false)
    private Integer amount; // in XAF
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;
    
    @Column(length = 255)
    private String description;
    
    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;
    
    @Column(name = "expense_date", nullable = false)
    @Builder.Default
    private LocalDate expenseDate = LocalDate.now();
    
    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    @Builder.Default
    private SplitType splitType = SplitType.EQUAL;
    
    @Column(name = "is_recurring")
    @Builder.Default
    private Boolean isRecurring = false;
    
    @Column(name = "recurring_day")
    private Integer recurringDay;
    
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseSplit> splits = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ExpenseCategory {
        RENT,
        UTILITIES,
        GROCERIES,
        SUPPLIES,
        INTERNET,
        CLEANING,
        WATER,
        ELECTRICITY,
        GAS,
        OTHER
    }
    
    public enum SplitType {
        EQUAL,      // Split equally among all members
        PERCENTAGE, // Split by rent share percentage
        FIXED,      // Fixed amounts per person
        CUSTOM      // Custom amounts
    }
    
    public void addSplit(ExpenseSplit split) {
        splits.add(split);
        split.setExpense(this);
    }
}
