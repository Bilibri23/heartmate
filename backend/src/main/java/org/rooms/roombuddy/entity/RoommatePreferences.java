package org.rooms.roombuddy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "roommate_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoommatePreferences {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    // Budget Preferences
    @Column(name = "min_budget")
    private Integer minBudget; // in XAF
    
    @Column(name = "max_budget")
    private Integer maxBudget; // in XAF
    
    // Location Preferences
    @Column(name = "preferred_locations", columnDefinition = "TEXT[]")
    private List<String> preferredLocations;
    
    @Column(name = "max_distance_from_campus", precision = 10, scale = 2)
    private BigDecimal maxDistanceFromCampus; // in kilometers
    
    // Lifestyle Preferences (1-5 scale)
    @Column(name = "cleanliness_level")
    private Integer cleanlinessLevel; // 1-5
    
    @Column(name = "noise_tolerance")
    private Integer noiseTolerance; // 1-5
    
    @Column(name = "social_level")
    private Integer socialLevel; // 1-5
    
    // Schedule Preferences
    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_schedule")
    private SleepSchedule sleepSchedule;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "study_time_preference")
    private StudyTimePreference studyTimePreference;
    
    // Habit Preferences
    private Boolean smoking;
    private Boolean drinking;
    private Boolean pets;
    private Boolean guests;
    private Boolean cooking;
    
    // Deal-breakers
    @Column(name = "deal_breakers", columnDefinition = "TEXT")
    private String dealBreakers;
    
    // Roommate Preferences
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_gender")
    private PreferredGender preferredGender;
    
    @Column(name = "min_age")
    private Integer minAge;
    
    @Column(name = "max_age")
    private Integer maxAge;
    
    @Column(name = "same_university")
    @Builder.Default
    private Boolean sameUniversity = false;
    
    @Column(name = "same_faculty")
    @Builder.Default
    private Boolean sameFaculty = false;
    
    // Status
    @Column(name = "looking_for_roommate")
    @Builder.Default
    private Boolean lookingForRoommate = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum SleepSchedule {
        EARLY_BIRD, NIGHT_OWL, FLEXIBLE
    }
    
    public enum StudyTimePreference {
        MORNING, AFTERNOON, EVENING, NIGHT, FLEXIBLE
    }
    
    public enum PreferredGender {
        MALE, FEMALE, ANY
    }
}

