package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.NeighborhoodReviewRequest;
import org.rooms.roombay.dto.response.NeighborhoodAssessmentResponse;
import org.rooms.roombay.dto.response.NeighborhoodReviewResponse;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.NeighborhoodReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** RoomBay 2.0 — Phase 5: crowdsourced neighborhood quality ratings (safety, amenities, transport, noise). */
@RestController
@RequestMapping("/api/neighborhoods")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Neighborhoods", description = "Crowdsourced neighborhood quality ratings")
public class NeighborhoodController {

    private final NeighborhoodReviewService neighborhoodReviewService;

    @PostMapping("/reviews")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit or update a neighborhood rating", description = "One rating per user per city+neighborhood; resubmitting updates it")
    public ResponseEntity<NeighborhoodReviewResponse> submitReview(@Valid @RequestBody NeighborhoodReviewRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(neighborhoodReviewService.submitOrUpdateReview(userId, request), HttpStatus.CREATED);
    }

    @GetMapping("/reviews")
    @Operation(summary = "List reviews for a neighborhood")
    public ResponseEntity<Page<NeighborhoodReviewResponse>> getReviews(
            @RequestParam String city,
            @RequestParam String neighborhood,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(neighborhoodReviewService.getReviews(city, neighborhood, pageable));
    }

    @GetMapping("/assessment")
    @Operation(summary = "Aggregate quality score for a neighborhood")
    public ResponseEntity<NeighborhoodAssessmentResponse> getAssessment(
            @RequestParam String city,
            @RequestParam String neighborhood) {
        return ResponseEntity.ok(neighborhoodReviewService.getAssessment(city, neighborhood));
    }
}
