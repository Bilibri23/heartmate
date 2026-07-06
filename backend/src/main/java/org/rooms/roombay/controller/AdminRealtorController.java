package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.RealtorReviewRequest;
import org.rooms.roombay.dto.response.RealtorProfileResponse;
import org.rooms.roombay.entity.RealtorProfile;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.RealtorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;

/** Admin review of realtor accounts (verification queue). */
@RestController
@RequestMapping("/api/admin/realtors")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Realtors", description = "Admin review of realtor verification")
public class AdminRealtorController {

    private final RealtorService realtorService;

    @GetMapping
    @Operation(summary = "List realtors by status", description = "Defaults to PENDING (the review queue)")
    public ResponseEntity<Page<RealtorProfileResponse>> list(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RealtorProfile.VerificationStatus parsed;
        try {
            parsed = RealtorProfile.VerificationStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            parsed = RealtorProfile.VerificationStatus.PENDING;
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return ResponseEntity.ok(realtorService.getByStatusForAdmin(parsed, pageable));
    }

    @GetMapping("/pending")
    @Operation(summary = "Realtor verification queue")
    public ResponseEntity<Page<RealtorProfileResponse>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return ResponseEntity.ok(
                realtorService.getByStatusForAdmin(RealtorProfile.VerificationStatus.PENDING, pageable));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a realtor")
    public ResponseEntity<RealtorProfileResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(realtorService.approve(id, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a realtor")
    public ResponseEntity<RealtorProfileResponse> reject(@PathVariable UUID id,
                                                         @RequestBody(required = false) RealtorReviewRequest request) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(realtorService.reject(id, SecurityUtils.getCurrentUserId(), reason));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend a realtor")
    public ResponseEntity<RealtorProfileResponse> suspend(@PathVariable UUID id,
                                                          @RequestBody(required = false) RealtorReviewRequest request) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(realtorService.suspend(id, SecurityUtils.getCurrentUserId(), reason));
    }
}
