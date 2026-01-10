package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.dto.response.PhotoDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {
    private UUID id;
    private UUID landlordId;
    private String landlordName;
    private String title;
    private String description;
    private String propertyType;
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
    private LocalDate availableFrom;
    private LocalDate availableTo;
    private String status;
    private Boolean verified;
    private Boolean featured;
    private Integer viewsCount;
    private Integer favoritesCount;
    private List<PhotoDTO> photos;
    private String primaryPhotoUrl;
    private Boolean isFavorite;
    private Double averageRating;
    private Integer reviewCount;
    private Integer compatibilityScore; // Match percentage for students viewing listings
    private String compatibilityReason; // Why this listing matches
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

