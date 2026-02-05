package org.rooms.roombuddy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks when a household member agreed to the house rules.
 */
@Entity
@Table(name = "rule_agreements",
       uniqueConstraints = @UniqueConstraint(columnNames = {"house_rules_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleAgreement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_rules_id", nullable = false)
    private HouseRules houseRules;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "agreed_at", nullable = false)
    @Builder.Default
    private LocalDateTime agreedAt = LocalDateTime.now();
    
    @Column
    @Builder.Default
    private Integer version = 1;
}
