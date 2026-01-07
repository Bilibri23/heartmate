package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {
    private UUID id;
    private UUID userId;
    private UUID matchedUserId;
    private String matchedUserFirstName;
    private String matchedUserLastName;
    private String matchedUserEmail;
    private String matchedUserProfilePhotoUrl;
    private String matchedUserWhatsapp; // Only visible on mutual match
    private Integer compatibilityScore;
    private Integer budgetScore;
    private Integer lifestyleScore;
    private Integer scheduleScore;
    private Integer locationScore;
    private Integer habitsScore;
    private String status;
    private String userAction; // ACCEPT, REJECT, or null
    private String matchedUserAction; // ACCEPT, REJECT, or null
    private Boolean isMutualMatch;
    private String explanation; // "You matched because: Similar budget, compatible schedules..."
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

