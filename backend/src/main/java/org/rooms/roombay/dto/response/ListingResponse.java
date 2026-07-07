package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.dto.response.PhotoDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {
    private UUID id;
    private UUID landlordId;
    private String landlordName;
    private String title;
    private String description;
    private String propertyType;
    private String listingPurpose;
    private String stayType;
    private String pricePeriod;
    private Integer salePrice;
    private Integer minStayDays;
    private Integer maxStayDays;
    private String furnishingStatus;
    private Boolean isSharedAccommodation;
    private Boolean roommatesWanted;
    private Integer availableRoommateSlots;
    private String sharedSpaceNotes;
    private Boolean roommateCompatibilityEnabled;
    private String preferredRoommateGender;
    private Integer rentAmount;
    private Integer deposit;
    private Integer agencyFees;
    private String region;
    private String city;
    private String neighborhood;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal distanceToUniversity;
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer squareMeters;
    private Integer floor;
    private List<String> amenities;
    private Boolean isUnfurnished;
    private Boolean roomPreviewEnabled;
    private BigDecimal roomLengthMeters;
    private BigDecimal roomWidthMeters;
    private BigDecimal roomHeightMeters;
    private String roomPreviewPhotoUrl;
    private String roomPreviewStatus;
    private LocalDate availableFrom;
    private LocalDate availableTo;
    private String status;
    private Boolean verified;
    private String ownershipDocumentUrl;
    /** LANDLORD or REALTOR — who manages this listing. */
    private String listedByRole;
    /** Set only when listedByRole is REALTOR. */
    private String agencyName;
    /**
     * Simplified trust signal for tenants: combines listing admin review and landlord KYC.
     * HIGH, VERIFIED, REVIEWED, STANDARD.
     */
    private String trustTier;
    /**
     * Tenant-facing trust indicators. Values only describe reviewed states, never private verification documents.
     */
    private Map<String, Boolean> trustSignals;
    private Boolean featured;
    private Integer viewsCount;
    private Integer favoritesCount;
    /** Populated for landlord listing aggregates (applications received per listing). */
    private Integer applicationsCount;
    private List<PhotoDTO> photos;
    private String primaryPhotoUrl;
    private Boolean isFavorite;
    private Double averageRating;
    private Integer reviewCount;
    private Integer compatibilityScore; // Match percentage for students viewing listings
    private String compatibilityReason; // Why this listing matches
    /**
     * Stable keys for "why you see this" chips (feed / discovery). Localized on the client.
     */
    private List<String> recommendationReasonCodes;
    /**
     * Internal quality diagnostics for landlord/admin surfaces only.
     */
    private Integer qualityScore;
    private Map<String, Boolean> qualitySignals;
    // Virtual Tour
    private String videoTourUrl;
    private String videoTourEmbedCode;
    private String virtualTourProvider; // panoee, kuula, zillow, cloudpano, generic
    private String videoTourThumbnail;
    private Integer videoTourDuration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
