package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "listing_ar_markers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingArMarker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private PropertyListing listing;

    @Column(name = "x_coord", nullable = false)
    private Double x;

    @Column(name = "y_coord", nullable = false)
    private Double y;

    @Column(name = "z_coord", nullable = false)
    private Double z;

    @Column(length = 120)
    private String label;

    @Column(length = 24)
    private String color;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
