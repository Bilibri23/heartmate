package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "property_listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyListing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;
    
    // Basic Information
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "property_type")
    private PropertyType propertyType;
    
    // Pricing
    @Column(name = "rent_amount", nullable = false)
    private Integer rentAmount; // in XAF
    
    @Column(name = "deposit")
    private Integer deposit; // in XAF
    
    @Column(name = "agency_fees")
    private Integer agencyFees; // in XAF
    
    // Location
    private String region;
    private String city;
    private String neighborhood;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(name = "distance_to_university", precision = 10, scale = 2)
    private BigDecimal distanceToUniversity; // in kilometers
    
    // Property Details
    private Integer bedrooms;
    private Integer bathrooms;
    
    @Column(name = "square_meters")
    private Integer squareMeters;
    
    private Integer floor;
    
    // Amenities
    @Column(columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> amenities;

    // Room Preview MVP
    @Column(name = "is_unfurnished")
    @Builder.Default
    private Boolean isUnfurnished = false;

    @Column(name = "room_preview_enabled")
    @Builder.Default
    private Boolean roomPreviewEnabled = false;

    @Column(name = "room_length_meters", precision = 6, scale = 2)
    private BigDecimal roomLengthMeters;

    @Column(name = "room_width_meters", precision = 6, scale = 2)
    private BigDecimal roomWidthMeters;

    @Column(name = "room_height_meters", precision = 6, scale = 2)
    private BigDecimal roomHeightMeters;

    @Column(name = "room_preview_photo_url", length = 500)
    private String roomPreviewPhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_preview_status")
    @Builder.Default
    private RoomPreviewStatus roomPreviewStatus = RoomPreviewStatus.NOT_ENABLED;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_purpose")
    @Builder.Default
    private ListingPurpose listingPurpose = ListingPurpose.RENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "stay_type")
    @Builder.Default
    private StayType stayType = StayType.LONG_TERM;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_period")
    @Builder.Default
    private PricePeriod pricePeriod = PricePeriod.MONTH;

    @Column(name = "sale_price")
    private Integer salePrice;

    @Column(name = "min_stay_days")
    private Integer minStayDays;

    @Column(name = "max_stay_days")
    private Integer maxStayDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "furnishing_status")
    @Builder.Default
    private FurnishingStatus furnishingStatus = FurnishingStatus.UNFURNISHED;

    @Column(name = "is_shared_accommodation")
    @Builder.Default
    private Boolean isSharedAccommodation = false;

    @Column(name = "roommates_wanted")
    @Builder.Default
    private Boolean roommatesWanted = false;

    @Column(name = "available_roommate_slots")
    private Integer availableRoommateSlots;

    @Column(name = "shared_space_notes", columnDefinition = "TEXT")
    private String sharedSpaceNotes;

    @Column(name = "roommate_compatibility_enabled")
    @Builder.Default
    private Boolean roommateCompatibilityEnabled = false;

    @Column(name = "preferred_roommate_gender")
    private String preferredRoommateGender;
    
    // Virtual Tour
    @Column(name = "video_tour_url")
    private String videoTourUrl;
    
    @Column(name = "video_tour_embed_code")
    private String videoTourEmbedCode;
    
    @Column(name = "virtual_tour_provider")
    private String virtualTourProvider; // panoee, kuula, zillow, cloudpano, generic
    
    @Column(name = "video_tour_thumbnail")
    private String videoTourThumbnail;
    
    @Column(name = "video_tour_duration")
    private Integer videoTourDuration; // in seconds
    
    // Availability
    @Column(name = "available_from")
    private LocalDate availableFrom;
    
    @Column(name = "available_to")
    private LocalDate availableTo;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.DRAFT;
    
    @Builder.Default
    private Boolean verified = false;
    
    @Builder.Default
    private Boolean featured = false;
    
    // Contact
    @Column(name = "landlord_whatsapp")
    private String landlordWhatsapp;
    
    // Statistics
    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;
    
    @Column(name = "favorites_count")
    @Builder.Default
    private Integer favoritesCount = 0;
    
    // Ratings
    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;
    
    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;
    
    // Admin
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum PropertyType {
        APARTMENT, HOUSE, STUDIO, ROOM, SHARED_ROOM, PRIVATE_ROOM
    }

    public enum ListingPurpose {
        RENT, SALE
    }

    public enum StayType {
        SHORT_TERM, LONG_TERM, FLEXIBLE
    }

    public enum PricePeriod {
        NIGHT, WEEK, MONTH, TOTAL
    }

    public enum FurnishingStatus {
        FURNISHED, SEMI_FURNISHED, UNFURNISHED
    }
    
    public enum Status {
        DRAFT, PENDING, ACTIVE, RENTED, INACTIVE, DELETED
    }

    public enum RoomPreviewStatus {
        NOT_ENABLED, PENDING_REVIEW, APPROVED, NEEDS_BETTER_PHOTO
    }
}
