package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    private String propertyType; // APARTMENT, HOUSE, STUDIO, SHARED_ROOM, PRIVATE_ROOM
    
    @NotNull(message = "Rent amount is required")
    @Min(value = 0, message = "Rent amount must be positive")
    private Integer rentAmount;
    
    @Min(value = 0, message = "Deposit must be positive")
    private Integer deposit;
    
    @Min(value = 0, message = "Agency fees must be positive")
    private Integer agencyFees;
    
    // Location
    private String region;
    private String city;
    private String neighborhood;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal distanceToUniversity;
    
    // Property Details
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer squareMeters;
    private Integer floor;
    
    // Amenities
    private List<String> amenities; // WiFi, Water, Electricity, Parking, Security, Furnished
    
    // Virtual Tour
    private String videoTourUrl;
    private String videoTourEmbedCode;
    private String virtualTourProvider; // panoee, kuula, zillow, cloudpano, generic
    private String videoTourThumbnail;
    private Integer videoTourDuration;
    
    // Availability
    private LocalDate availableFrom;
    private LocalDate availableTo;
    
    // Contact
    private String landlordWhatsapp;
    
    // Status (for landlord to set)
    private String status; // DRAFT, PENDING, ACTIVE, RENTED, INACTIVE, DELETED
}

