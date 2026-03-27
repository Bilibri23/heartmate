package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseRequest {
    
    @NotNull(message = "Application ID is required")
    private UUID applicationId;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;
    
    @NotNull(message = "Monthly rent is required")
    @Positive(message = "Monthly rent must be positive")
    private Integer monthlyRent;
    
    @Positive(message = "Deposit must be positive")
    private Integer depositAmount;
    
    @Positive(message = "Agency fees must be positive")
    private Integer agencyFees;
    
    private String termsContent;
}
