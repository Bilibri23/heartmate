package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.RoomApplication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for room application responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomApplicationResponse {
    
    private UUID id;
    private UUID listingId;
    private String listingTitle;
    private String listingAddress;
    private Integer listingPrice;
    private String listingCity;
    private String listingPrimaryPhotoUrl;
    
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    private String studentProfilePhotoUrl;
    private Boolean studentVerified;
    
    // Co-application fields
    private Boolean isCoApplication;
    private UUID coApplicantId;
    private String coApplicantName;
    private String coApplicantEmail;
    private String coApplicantPhone;
    private String coApplicantProfilePhotoUrl;
    private Boolean coApplicantVerified;
    private Boolean coApplicantConfirmed;
    private LocalDateTime coApplicantConfirmedAt;
    
    private String message;
    private LocalDate moveInDate;
    private Integer leaseDurationMonths;
    
    private RoomApplication.Status status;
    private String landlordResponse;
    private LocalDateTime landlordViewedAt;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    
    // Computed fields
    private Boolean isActive;
    private Boolean isReviewed;
    private Long daysSinceApplication;
    
    /**
     * Create response from entity
     */
    public static RoomApplicationResponse fromEntity(RoomApplication application) {
        if (application == null) {
            return null;
        }
        
        RoomApplicationResponseBuilder builder = RoomApplicationResponse.builder()
                .id(application.getId())
                .message(application.getMessage())
                .moveInDate(application.getMoveInDate())
                .leaseDurationMonths(application.getLeaseDurationMonths())
                .status(application.getStatus())
                .landlordResponse(application.getLandlordResponse())
                .landlordViewedAt(application.getLandlordViewedAt())
                .reviewedAt(application.getReviewedAt())
                .rejectionReason(application.getRejectionReason())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .expiresAt(application.getExpiresAt())
                .isActive(application.isActive())
                .isReviewed(application.isReviewed())
                .isCoApplication(application.getIsCoApplication())
                .coApplicantConfirmed(application.getCoApplicantConfirmed())
                .coApplicantConfirmedAt(application.getCoApplicantConfirmedAt());
        
        // Listing info
        if (application.getListing() != null) {
            builder.listingId(application.getListing().getId())
                   .listingTitle(application.getListing().getTitle())
                   .listingAddress(application.getListing().getAddress())
                   .listingPrice(application.getListing().getRentAmount())
                   .listingCity(application.getListing().getCity());
            
            // Note: Primary photo will be set by the service layer
            // since ListingPhoto is a separate entity
        }
        
        // Student info
        if (application.getStudent() != null) {
            builder.studentId(application.getStudent().getId())
                   .studentName(application.getStudent().getFirstName() + " " + application.getStudent().getLastName())
                   .studentEmail(application.getStudent().getEmail())
                   .studentPhone(application.getStudent().getPhone());
            
            // Note: Student verification status and profile photo will be set by the service layer
            // since these are separate entities without back-references
        }
        
        // Co-applicant info
        if (application.getCoApplicant() != null) {
            builder.coApplicantId(application.getCoApplicant().getId())
                   .coApplicantName(application.getCoApplicant().getFirstName() + " " + application.getCoApplicant().getLastName())
                   .coApplicantEmail(application.getCoApplicant().getEmail())
                   .coApplicantPhone(application.getCoApplicant().getPhone());
            
            // Note: Co-applicant verification status and profile photo will be set by the service layer
        }
        
        // Calculate days since application
        if (application.getCreatedAt() != null) {
            long days = java.time.Duration.between(application.getCreatedAt(), LocalDateTime.now()).toDays();
            builder.daysSinceApplication(days);
        }
        
        return builder.build();
    }
}
