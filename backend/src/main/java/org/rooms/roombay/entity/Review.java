package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reviews and ratings for landlords, listings, and roommates
 * Only allowed after a completed or terminated lease
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
    @UniqueConstraint(name = "uk_review_unique", columnNames = {"reviewer_id", "lease_id", "review_type"})
})
@SuppressWarnings("JpaDataSourceORMInspection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee; // Person being reviewed
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease; // Must have a completed lease to review
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private PropertyListing listing; // For listing reviews
    
    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false)
    private ReviewType reviewType;
    
    // Ratings (1-5 stars)
    @Column(name = "overall_rating", nullable = false)
    private Integer overallRating; // 1-5
    
    // Specific ratings based on review type
    @Column(name = "communication_rating")
    private Integer communicationRating; // How responsive
    
    @Column(name = "accuracy_rating")
    private Integer accuracyRating; // Listing accuracy / honesty
    
    @Column(name = "cleanliness_rating")
    private Integer cleanlinessRating; // Property/person cleanliness
    
    @Column(name = "value_rating")
    private Integer valueRating; // Value for money
    
    @Column(name = "location_rating")
    private Integer locationRating; // Location accuracy
    
    // Review content
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    // Pros and cons
    @Column(columnDefinition = "TEXT")
    private String pros;
    
    @Column(columnDefinition = "TEXT")
    private String cons;
    
    // Would recommend?
    @Column(name = "would_recommend")
    @Builder.Default
    private Boolean wouldRecommend = true;
    
    // Moderation
    @Builder.Default
    private Boolean visible = true;
    
    @Builder.Default
    private Boolean flagged = false;
    
    @Column(name = "flag_reason")
    private String flagReason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by")
    private User moderatedBy;
    
    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;
    
    // Response from reviewee
    @Column(name = "response", columnDefinition = "TEXT")
    private String response;
    
    @Column(name = "response_at")
    private LocalDateTime responseAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ReviewType {
        STUDENT_TO_LANDLORD,  // Student reviews landlord
        STUDENT_TO_LISTING,   // Student reviews the property
        LANDLORD_TO_STUDENT,  // Landlord reviews student
        ROOMMATE_TO_ROOMMATE  // Roommate reviews roommate
    }
    
    public double getAverageRating() {
        int count = 1; // overall is always present
        int sum = overallRating;
        
        if (communicationRating != null) { sum += communicationRating; count++; }
        if (accuracyRating != null) { sum += accuracyRating; count++; }
        if (cleanlinessRating != null) { sum += cleanlinessRating; count++; }
        if (valueRating != null) { sum += valueRating; count++; }
        if (locationRating != null) { sum += locationRating; count++; }
        
        return (double) sum / count;
    }
}
