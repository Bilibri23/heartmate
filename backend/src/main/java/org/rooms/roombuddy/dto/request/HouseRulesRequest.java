package org.rooms.roombuddy.dto.request;

import lombok.Data;
import org.rooms.roombuddy.entity.HouseRules;

import java.time.LocalTime;
import java.util.List;

@Data
public class HouseRulesRequest {
    
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
}
