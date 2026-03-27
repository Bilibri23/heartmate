package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.Review;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    
    @NotNull(message = "Lease ID is required")
    private UUID leaseId;
    
    @NotNull(message = "Review type is required")
    private Review.ReviewType reviewType;
    
    @NotNull(message = "Overall rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer overallRating;
    
    @Min(value = 1) @Max(value = 5)
    private Integer communicationRating;
    
    @Min(value = 1) @Max(value = 5)
    private Integer accuracyRating;
    
    @Min(value = 1) @Max(value = 5)
    private Integer cleanlinessRating;
    
    @Min(value = 1) @Max(value = 5)
    private Integer valueRating;
    
    @Min(value = 1) @Max(value = 5)
    private Integer locationRating;
    
    @Size(max = 2000, message = "Comment must be less than 2000 characters")
    private String comment;
    
    @Size(max = 500)
    private String pros;
    
    @Size(max = 500)
    private String cons;
    
    private Boolean wouldRecommend;
}
