package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesResponse {
    private UUID id;
    private UUID userId;
    private Integer minBudget;
    private Integer maxBudget;
    private List<String> preferredLocations;
    private BigDecimal maxDistanceFromCampus;
    private Integer cleanlinessLevel;
    private Integer noiseTolerance;
    private Integer socialLevel;
    private String sleepSchedule;
    private String studyTimePreference;
    private Boolean smoking;
    private Boolean drinking;
    private Boolean pets;
    private Boolean guests;
    private Boolean cooking;
    private String dealBreakers;
    private String preferredGender;
    private Integer minAge;
    private Integer maxAge;
    private Boolean sameUniversity;
    private Boolean sameFaculty;
    private Boolean lookingForRoommate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

