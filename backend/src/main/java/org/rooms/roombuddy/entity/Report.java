package org.rooms.roombuddy.entity;

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
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "reports")
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reported_entity_type", nullable = false)
    private EntityType reportedEntityType;

    @Column(name = "reported_entity_id", nullable = false)
    private UUID reportedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportPriority priority = ReportPriority.MEDIUM;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken")
    private ReportAction actionTaken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum EntityType {
        USER, LISTING
    }

    public enum ReportReason {
        SPAM,
        FRAUD,
        INAPPROPRIATE,
        FAKE,
        SCAM,
        HARASSMENT,
        MISLEADING,
        DUPLICATE,
        OTHER
    }

    public enum ReportStatus {
        PENDING,
        REVIEWING,
        RESOLVED,
        DISMISSED
    }

    public enum ReportPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum ReportAction {
        REMOVED,
        WARNING,
        SUSPENDED,
        BANNED,
        NO_ACTION,
        LISTING_DELETED,
        USER_WARNED
    }
}
