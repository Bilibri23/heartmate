package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating a room application
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomApplicationRequest {
    
    @NotNull(message = "Listing ID is required")
    private UUID listingId;
    
    @NotBlank(message = "Message is required")
    @Size(min = 50, max = 1000, message = "Message must be between 50 and 1000 characters")
    private String message;
    
    @NotNull(message = "Move-in date is required")
    @Future(message = "Move-in date must be in the future")
    private LocalDate moveInDate;
    
    @NotNull(message = "Lease duration is required")
    @Min(value = 1, message = "Lease duration must be at least 1 month")
    @Max(value = 24, message = "Lease duration cannot exceed 24 months")
    private Integer leaseDurationMonths;
}
