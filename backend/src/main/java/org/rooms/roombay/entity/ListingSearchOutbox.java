package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Transactional outbox entry for syncing a listing change to the search index
 * (Elasticsearch/OpenSearch/Meilisearch). Processed by a background worker.
 */
@Entity
@Table(name = "listing_search_outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingSearchOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // LISTING_CREATED, LISTING_UPDATED, LISTING_DELETED

    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processing_attempts")
    @Builder.Default
    private Integer processingAttempts = 0;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static final String EVENT_CREATED = "LISTING_CREATED";
    public static final String EVENT_UPDATED = "LISTING_UPDATED";
    public static final String EVENT_DELETED = "LISTING_DELETED";
}
