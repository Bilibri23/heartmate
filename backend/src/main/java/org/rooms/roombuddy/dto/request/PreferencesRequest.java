package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesRequest {
    
    // Budget Preferences
    @Min(value = 0, message = "Min budget must be positive")
    private Integer minBudget;
    
    @Min(value = 0, message = "Max budget must be positive")
    private Integer maxBudget;
    
    // Location Preferences
    private List<String> preferredLocations;
    
    @DecimalMin(value = "0.0", message = "Max distance must be positive")
    private BigDecimal maxDistanceFromCampus;
    
    // Lifestyle Preferences (1-5 scale)
    @Min(value = 1, message = "Cleanliness level must be between 1 and 5")
    @Max(value = 5, message = "Cleanliness level must be between 1 and 5")
    private Integer cleanlinessLevel;
    
    @Min(value = 1, message = "Noise tolerance must be between 1 and 5")
    @Max(value = 5, message = "Noise tolerance must be between 1 and 5")
    private Integer noiseTolerance;
    
    @Min(value = 1, message = "Social level must be between 1 and 5")
    @Max(value = 5, message = "Social level must be between 1 and 5")
    private Integer socialLevel;
    
    // Schedule Preferences
    private String sleepSchedule; // EARLY_BIRD, NIGHT_OWL, FLEXIBLE
    private String studyTimePreference; // MORNING, AFTERNOON, EVENING, NIGHT, FLEXIBLE
    
    // Habit Preferences
    private Boolean smoking;
    private Boolean drinking;
    private Boolean pets;
    private Boolean guests;
    private Boolean cooking;
    
    // Deal-breakers
    private String dealBreakers;
    
    // Roommate Preferences
    private String preferredGender; // MALE, FEMALE, ANY
    private Integer minAge;
    private Integer maxAge;
    private Boolean sameUniversity;
    private Boolean sameFaculty;
    
    // Status
    private Boolean lookingForRoommate;
}

