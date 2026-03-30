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

@Entity
@Table(name = "matches", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user1_id", "user2_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;
    
    @Column(name = "compatibility_score", nullable = false)
    private Integer compatibilityScore; // 0-100
    
    // Compatibility breakdown scores
    @Column(name = "budget_score")
    private Integer budgetScore;
    
    @Column(name = "lifestyle_score")
    private Integer lifestyleScore;
    
    @Column(name = "schedule_score")
    private Integer scheduleScore;
    
    @Column(name = "location_score")
    private Integer locationScore;
    
    @Column(name = "habits_score")
    private Integer habitsScore;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user1_action")
    private UserAction user1Action;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user2_action")
    private UserAction user2Action;
    
    @Column(name = "algorithm_version")
    @Builder.Default
    private String algorithmVersion = "1.0";
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    public enum Status {
        PENDING, ACCEPTED, REJECTED, EXPIRED, MUTUAL
    }
    
    public enum UserAction {
        ACCEPT, REJECT
    }
    
    public boolean isMutualMatch() {
        return user1Action == UserAction.ACCEPT && user2Action == UserAction.ACCEPT;
    }
    
    public boolean isRejected() {
        return user1Action == UserAction.REJECT || user2Action == UserAction.REJECT;
    }
}

