package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.ReviewRequest;
import org.rooms.roombuddy.dto.response.ReviewResponse;
import org.rooms.roombuddy.dto.response.ReviewStatsResponse;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reviews", description = "Review and rating APIs")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {
    
    private final ReviewService reviewService;
    
    @PostMapping
    @Operation(summary = "Create review", description = "Create a review after lease completion")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        UUID reviewerId = SecurityUtils.getCurrentUserId();
        log.info("User {} creating review for lease {}", reviewerId, request.getLeaseId());
        ReviewResponse review = reviewService.createReview(reviewerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }
    
    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review by ID")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable UUID reviewId) {
        ReviewResponse review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get reviews for a user", description = "Get all visible reviews for a user")
    public ResponseEntity<Page<ReviewResponse>> getReviewsForUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponse> reviews = reviewService.getReviewsForUser(userId, pageable);
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/listing/{listingId}")
    @Operation(summary = "Get reviews for a listing")
    public ResponseEntity<Page<ReviewResponse>> getReviewsForListing(
            @PathVariable UUID listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponse> reviews = reviewService.getReviewsForListing(listingId, pageable);
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/my")
    @Operation(summary = "Get my reviews", description = "Get reviews I have written")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponse> reviews = reviewService.getMyReviews(userId, pageable);
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/stats/{userId}")
    @Operation(summary = "Get review stats for a user")
    public ResponseEntity<ReviewStatsResponse> getReviewStats(@PathVariable UUID userId) {
        ReviewStatsResponse stats = reviewService.getReviewStatsForUser(userId);
        return ResponseEntity.ok(stats);
    }
    
    @PostMapping("/{reviewId}/respond")
    @Operation(summary = "Respond to a review", description = "Reviewee can respond to a review")
    public ResponseEntity<ReviewResponse> respondToReview(
            @PathVariable UUID reviewId,
            @RequestBody String response) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} responding to review {}", userId, reviewId);
        ReviewResponse review = reviewService.respondToReview(reviewId, userId, response);
        return ResponseEntity.ok(review);
    }
    
    @PostMapping("/{reviewId}/flag")
    @Operation(summary = "Flag a review", description = "Flag a review for moderation")
    public ResponseEntity<Void> flagReview(
            @PathVariable UUID reviewId,
            @RequestParam String reason) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} flagging review {} for: {}", userId, reviewId, reason);
        reviewService.flagReview(reviewId, userId, reason);
        return ResponseEntity.ok().build();
    }
}
