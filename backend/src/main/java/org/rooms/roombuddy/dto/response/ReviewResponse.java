package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.Review;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    
    private UUID id;
    
    // Reviewer info
    private UUID reviewerId;
    private String reviewerName;
    private String reviewerProfilePhoto;
    
    // Reviewee info
    private UUID revieweeId;
    private String revieweeName;
    
    // Listing info (if applicable)
    private UUID listingId;
    private String listingTitle;
    
    private Review.ReviewType reviewType;
    
    // Ratings
    private Integer overallRating;
    private Integer communicationRating;
    private Integer accuracyRating;
    private Integer cleanlinessRating;
    private Integer valueRating;
    private Integer locationRating;
    private Double averageRating;
    
    // Content
    private String comment;
    private String pros;
    private String cons;
    private Boolean wouldRecommend;
    
    // Response from reviewee
    private String response;
    private LocalDateTime responseAt;
    
    // Timestamps
    private LocalDateTime createdAt;
}
