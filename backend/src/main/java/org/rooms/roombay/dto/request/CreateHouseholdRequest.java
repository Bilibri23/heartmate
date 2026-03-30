package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateHouseholdRequest {
    
    @NotBlank(message = "Household name is required")
    @Size(max = 100)
    private String name;
    
    private String description;
    
    private UUID leaseId;
    
    private UUID listingId;
    
    private List<UUID> memberIds; // Initial members to invite
}
