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
 * Represents a shared living arrangement between roommates.
 * Auto-created when multiple tenants sign leases for the same shared listing.
 */
@Entity
@Table(name = "households")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Household {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id")
    private Lease lease;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private PropertyListing listing;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private HouseholdStatus status = HouseholdStatus.ACTIVE;
    
    @OneToMany(mappedBy = "household", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HouseholdMember> members = new ArrayList<>();
    
    @OneToMany(mappedBy = "household", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HouseholdExpense> expenses = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum HouseholdStatus {
        ACTIVE,     // Household is active
        INACTIVE,   // All members have left
        ARCHIVED    // Lease ended
    }
    
    public void addMember(HouseholdMember member) {
        members.add(member);
        member.setHousehold(this);
    }
    
    public void removeMember(HouseholdMember member) {
        members.remove(member);
        member.setHousehold(null);
    }
}
