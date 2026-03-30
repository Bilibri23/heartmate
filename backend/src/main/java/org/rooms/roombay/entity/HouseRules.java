package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * House rules that all household members agree to follow.
 */
@Entity
@Table(name = "house_rules",
       uniqueConstraints = @UniqueConstraint(columnNames = {"household_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseRules {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;
    
    // Quiet hours
    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private Boolean quietHoursEnabled = false;
    
    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;
    
    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;
    
    // Guest policy
    @Enumerated(EnumType.STRING)
    @Column(name = "guest_policy", length = 30)
    @Builder.Default
    private GuestPolicy guestPolicy = GuestPolicy.NOTIFY;
    
    // Cleaning
    @Enumerated(EnumType.STRING)
    @Column(name = "cleaning_schedule", length = 30)
    @Builder.Default
    private CleaningSchedule cleaningSchedule = CleaningSchedule.ROTATING;
    
    // Smoking/Drinking
    @Column(name = "smoking_allowed")
    @Builder.Default
    private Boolean smokingAllowed = false;
    
    @Column(name = "smoking_area", length = 100)
    private String smokingArea;
    
    @Column(name = "drinking_allowed")
    @Builder.Default
    private Boolean drinkingAllowed = true;
    
    // Pets
    @Column(name = "pets_allowed")
    @Builder.Default
    private Boolean petsAllowed = false;
    
    @Column(name = "pet_rules", length = 255)
    private String petRules;
    
    // Shared spaces
    @Column(name = "kitchen_rules", columnDefinition = "TEXT")
    private String kitchenRules;
    
    @Column(name = "bathroom_rules", columnDefinition = "TEXT")
    private String bathroomRules;
    
    @Column(name = "living_room_rules", columnDefinition = "TEXT")
    private String livingRoomRules;
    
    // Custom rules as JSON array
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_rules", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> customRules = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by")
    private User lastUpdatedBy;
    
    @OneToMany(mappedBy = "houseRules", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RuleAgreement> agreements = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum GuestPolicy {
        ALWAYS_OK,      // Guests welcome anytime
        NOTIFY,         // Notify roommates when having guests
        ASK_FIRST,      // Ask permission first
        NO_OVERNIGHT,   // No overnight guests
        NO_GUESTS       // No guests allowed
    }
    
    public enum CleaningSchedule {
        ROTATING,   // Members take turns
        ASSIGNED,   // Fixed assignments
        FLEXIBLE,   // Clean as needed
        SHARED      // Clean together
    }
}
