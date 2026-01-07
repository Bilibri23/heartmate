package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.ReviewRequest;
import org.rooms.roombuddy.dto.response.ReviewResponse;
import org.rooms.roombuddy.dto.response.ReviewStatsResponse;
import org.rooms.roombuddy.entity.Lease;
import org.rooms.roombuddy.entity.Review;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.LeaseRepository;
import org.rooms.roombuddy.repository.ReviewRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public ReviewResponse createReview(UUID reviewerId, ReviewRequest request) {
        log.info("Creating review for lease: {} by user: {}", request.getLeaseId(), reviewerId);
        
        Lease lease = leaseRepository.findById(request.getLeaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        // Verify lease can be reviewed
        if (!lease.canBeReviewed()) {
            throw new BadRequestException("This lease cannot be reviewed yet. It must be completed or terminated.");
        }
        
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Determine reviewee based on review type
        User reviewee;
        switch (request.getReviewType()) {
            case STUDENT_TO_LANDLORD:
            case STUDENT_TO_LISTING:
                if (!lease.getStudent().getId().equals(reviewerId)) {
                    throw new BadRequestException("Only the student can write this type of review");
                }
                reviewee = lease.getLandlord();
                break;
            case LANDLORD_TO_STUDENT:
                if (!lease.getLandlord().getId().equals(reviewerId)) {
                    throw new BadRequestException("Only the landlord can write this type of review");
                }
                reviewee = lease.getStudent();
                break;
            default:
                throw new BadRequestException("Invalid review type");
        }
        
        // Check if already reviewed
        if (reviewRepository.existsByReviewerIdAndLeaseIdAndReviewType(reviewerId, lease.getId(), request.getReviewType())) {
            throw new BadRequestException("You have already submitted this type of review for this lease");
        }
        
        Review review = Review.builder()
                .reviewer(reviewer)
                .reviewee(reviewee)
                .lease(lease)
                .listing(request.getReviewType() == Review.ReviewType.STUDENT_TO_LISTING ? lease.getListing() : null)
                .reviewType(request.getReviewType())
                .overallRating(request.getOverallRating())
                .communicationRating(request.getCommunicationRating())
                .accuracyRating(request.getAccuracyRating())
                .cleanlinessRating(request.getCleanlinessRating())
                .valueRating(request.getValueRating())
                .locationRating(request.getLocationRating())
                .comment(request.getComment())
                .pros(request.getPros())
                .cons(request.getCons())
                .wouldRecommend(request.getWouldRecommend() != null ? request.getWouldRecommend() : true)
                .visible(true)
                .flagged(false)
                .build();
        
        Review saved = reviewRepository.save(review);
        log.info("Review created: {}", saved.getId());
        
        // Notify the reviewee about the new review
        notificationService.notifyReviewReceived(
                reviewee.getId(),
                saved.getId(),
                reviewer.getFirstName() + " " + reviewer.getLastName()
        );
        
        return mapToResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        return mapToResponse(review);
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForUser(UUID userId, Pageable pageable) {
        return reviewRepository.findByRevieweeIdAndVisibleTrue(userId, pageable).map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForListing(UUID listingId, Pageable pageable) {
        return reviewRepository.findByListingIdAndVisibleTrue(listingId, pageable).map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(UUID userId, Pageable pageable) {
        return reviewRepository.findByReviewerId(userId, pageable).map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public ReviewStatsResponse getReviewStatsForUser(UUID userId) {
        Double avgRating = reviewRepository.getAverageRatingForUser(userId);
        long totalReviews = reviewRepository.countReviewsForUser(userId);
        List<Object[]> distribution = reviewRepository.getRatingDistributionForUser(userId);
        
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }
        for (Object[] row : distribution) {
            ratingDistribution.put((Integer) row[0], (Long) row[1]);
        }
        
        return ReviewStatsResponse.builder()
                .userId(userId)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalReviews(totalReviews)
                .ratingDistribution(ratingDistribution)
                .build();
    }
    
    @Transactional(readOnly = true)
    public Double getAverageRatingForListing(UUID listingId) {
        return reviewRepository.getAverageRatingForListing(listingId);
    }
    
    public ReviewResponse respondToReview(UUID reviewId, UUID userId, String response) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        if (!review.getReviewee().getId().equals(userId)) {
            throw new BadRequestException("Only the person being reviewed can respond");
        }
        
        if (review.getResponse() != null) {
            throw new BadRequestException("You have already responded to this review");
        }
        
        review.setResponse(response);
        review.setResponseAt(LocalDateTime.now());
        
        return mapToResponse(reviewRepository.save(review));
    }
    
    public void flagReview(UUID reviewId, UUID userId, String reason) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        review.setFlagged(true);
        review.setFlagReason(reason);
        reviewRepository.save(review);
        
        log.info("Review {} flagged by user {} for: {}", reviewId, userId, reason);
    }
    
    // Admin methods
    public ReviewResponse moderateReview(UUID reviewId, UUID adminId, boolean visible) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        
        review.setVisible(visible);
        review.setModeratedBy(admin);
        review.setModeratedAt(LocalDateTime.now());
        review.setFlagged(false);
        
        log.info("Review {} moderated by admin {}, visible: {}", reviewId, adminId, visible);
        
        return mapToResponse(reviewRepository.save(review));
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getFlaggedReviews(Pageable pageable) {
        return reviewRepository.findByFlaggedTrue(pageable).map(this::mapToResponse);
    }
    
    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName())
                .revieweeId(review.getReviewee().getId())
                .revieweeName(review.getReviewee().getFirstName() + " " + review.getReviewee().getLastName())
                .listingId(review.getListing() != null ? review.getListing().getId() : null)
                .listingTitle(review.getListing() != null ? review.getListing().getTitle() : null)
                .reviewType(review.getReviewType())
                .overallRating(review.getOverallRating())
                .communicationRating(review.getCommunicationRating())
                .accuracyRating(review.getAccuracyRating())
                .cleanlinessRating(review.getCleanlinessRating())
                .valueRating(review.getValueRating())
                .locationRating(review.getLocationRating())
                .averageRating(review.getAverageRating())
                .comment(review.getComment())
                .pros(review.getPros())
                .cons(review.getCons())
                .wouldRecommend(review.getWouldRecommend())
                .response(review.getResponse())
                .responseAt(review.getResponseAt())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
