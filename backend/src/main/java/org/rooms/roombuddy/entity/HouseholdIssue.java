package org.rooms.roombuddy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reported issues and conflicts within the household.
 */
@Entity
@Table(name = "household_issues")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdIssue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_against")
    private User reportedAgainst; // Optional
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueCategory category;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IssueSeverity severity = IssueSeverity.MEDIUM;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IssueStatus status = IssueStatus.OPEN;
    
    @Column(columnDefinition = "TEXT")
    private String resolution;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
    
    @Column(name = "is_escalated")
    @Builder.Default
    private Boolean isEscalated = false;
    
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;
    
    @Column(name = "escalated_to", length = 50)
    private String escalatedTo;
    
    @Column(name = "is_anonymous")
    @Builder.Default
    private Boolean isAnonymous = false;
    
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IssueComment> comments = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum IssueCategory {
        NOISE,
        CLEANLINESS,
        BILLS,
        GUESTS,
        BEHAVIOR,
        PROPERTY,
        RULES_VIOLATION,
        OTHER
    }
    
    public enum IssueSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    public enum IssueStatus {
        OPEN,
        ACKNOWLEDGED,
        IN_DISCUSSION,
        RESOLVED,
        ESCALATED,
        CLOSED
    }
    
    public void resolve(User resolver, String resolution) {
        this.resolvedBy = resolver;
        this.resolution = resolution;
        this.resolvedAt = LocalDateTime.now();
        this.status = IssueStatus.RESOLVED;
    }
    
    public void escalate(String to) {
        this.isEscalated = true;
        this.escalatedAt = LocalDateTime.now();
        this.escalatedTo = to;
        this.status = IssueStatus.ESCALATED;
    }
}
