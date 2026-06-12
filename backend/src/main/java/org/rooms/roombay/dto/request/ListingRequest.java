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

    public interface Create {}
    public interface Update {}
    
    @NotBlank(message = "Title is required", groups = Create.class)
    private String title;
    
    private String description;
    
    private String propertyType; // APARTMENT, HOUSE, STUDIO, ROOM, SHARED_ROOM, PRIVATE_ROOM

    private String listingPurpose; // RENT, SALE
    private String stayType; // SHORT_TERM, LONG_TERM, FLEXIBLE
    private String pricePeriod; // NIGHT, WEEK, MONTH, TOTAL
    private Integer salePrice;
    private Integer minStayDays;
    private Integer maxStayDays;
    private String furnishingStatus; // FURNISHED, SEMI_FURNISHED, UNFURNISHED
    
    @Min(value = 0, message = "Rent amount must be positive", groups = {Create.class, Update.class})
    private Integer rentAmount;
    
    @Min(value = 0, message = "Deposit must be positive", groups = {Create.class, Update.class})
    private Integer deposit;
    
    @Min(value = 0, message = "Agency fees must be positive", groups = {Create.class, Update.class})
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

      // Shared accommodation
    private Boolean isSharedAccommodation;
    private Boolean roommatesWanted;
    private Integer availableRoommateSlots;
    private String sharedSpaceNotes;
    private Boolean roommateCompatibilityEnabled;
    private String preferredRoommateGender;

    // Room Preview
    private Boolean isUnfurnished;
    private Boolean roomPreviewEnabled;
    private BigDecimal roomLengthMeters;
    private BigDecimal roomWidthMeters;
    private BigDecimal roomHeightMeters;
    private String roomPreviewPhotoUrl;
    private String roomPreviewStatus;
    
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
