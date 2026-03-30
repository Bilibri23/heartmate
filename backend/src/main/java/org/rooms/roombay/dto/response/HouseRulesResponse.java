package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.HouseRules;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseRulesResponse {
    
    private UUID id;
    private UUID householdId;
    
    // Quiet hours
    private Boolean quietHoursEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    
    // Guest policy
    private HouseRules.GuestPolicy guestPolicy;
    
    // Cleaning
    private HouseRules.CleaningSchedule cleaningSchedule;
    
    // Smoking/Drinking
    private Boolean smokingAllowed;
    private String smokingArea;
    private Boolean drinkingAllowed;
    
    // Pets
    private Boolean petsAllowed;
    private String petRules;
    
    // Shared spaces
    private String kitchenRules;
    private String bathroomRules;
    private String livingRoomRules;
    
    // Custom rules
    private List<String> customRules;
    
    // Agreement status
    private List<AgreementStatus> agreements;
    private Boolean allMembersAgreed;
    private Boolean currentUserAgreed;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastUpdatedByName;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgreementStatus {
        private UUID userId;
        private String userName;
        private String userPhoto;
        private Boolean hasAgreed;
        private LocalDateTime agreedAt;
    }
}
