package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.ApplicationReviewRequest;
import org.rooms.roombay.dto.request.RoomApplicationRequest;
import org.rooms.roombay.dto.response.RoomApplicationResponse;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.security.RequiresCompletion;
import org.rooms.roombay.security.RequiresVerification;
import org.rooms.roombay.dto.response.ApplicationTimelineResponse;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.ApplicationService;
import org.rooms.roombay.service.ApplicationTimelineService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Applications", description = "APIs for managing room applications")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {
    
    private final ApplicationService applicationService;
    private final ApplicationTimelineService applicationTimelineService;

    /**
     * Create a new application (Students only)
     */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @RequiresVerification(role = "STUDENT", verificationType = "STUDENT_ID")
    @RequiresCompletion(operation = "APPLY")
    @Operation(summary = "Apply to a listing", description = "Create a new application to a property listing (Students only)")
    public ResponseEntity<RoomApplicationResponse> createApplication(
            @Valid @RequestBody RoomApplicationRequest request) {
        
        UUID studentId = SecurityUtils.getCurrentUserId();
        log.info("Student {} applying to listing {}", studentId, request.getListingId());
        
        RoomApplicationResponse response = applicationService.createApplication(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get application by ID
     */
    @GetMapping("/{applicationId}")
    @Operation(summary = "Get application by ID", description = "Get a specific application (Student or Landlord)")
    public ResponseEntity<RoomApplicationResponse> getApplication(
            @PathVariable UUID applicationId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        RoomApplicationResponse response = applicationService.getApplication(applicationId, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get my applications (for students)
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get my applications", description = "Get all applications by the current student")
    public ResponseEntity<Page<RoomApplicationResponse>> getMyApplications(
            @RequestParam(required = false) RoomApplication.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        UUID studentId = SecurityUtils.getCurrentUserId();
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<RoomApplicationResponse> applications = applicationService.getStudentApplications(
            studentId, status, pageable);
        
        return ResponseEntity.ok(applications);
    }

    /**
     * Get my application for a listing (students only)
     */
    @GetMapping("/my/listing/{listingId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get my application for a listing", description = "Get the current student's application for a listing")
    public ResponseEntity<RoomApplicationResponse> getMyApplicationForListing(
            @PathVariable UUID listingId) {
        UUID studentId = SecurityUtils.getCurrentUserId();
        try {
            RoomApplicationResponse response = applicationService.getStudentApplicationForListing(studentId, listingId);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.noContent().build();
        }
    }
    
    /**
     * Get applications for my listings (for landlords)
     */
    @GetMapping("/landlord/received")
    @PreAuthorize("hasRole('LANDLORD')")
    @Operation(summary = "Get received applications", description = "Get all applications for landlord's listings")
    public ResponseEntity<Page<RoomApplicationResponse>> getReceivedApplications(
            @RequestParam(required = false) RoomApplication.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        UUID landlordId = SecurityUtils.getCurrentUserId();
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<RoomApplicationResponse> applications = applicationService.getLandlordApplications(
            landlordId, status, pageable);
        
        return ResponseEntity.ok(applications);
    }
    
    /**
     * Get applications for a specific listing (for landlords)
     */
    @GetMapping("/listing/{listingId}")
    @PreAuthorize("hasRole('LANDLORD')")
    @Operation(summary = "Get applications for a listing", description = "Get all applications for a specific listing")
    public ResponseEntity<Page<RoomApplicationResponse>> getListingApplications(
            @PathVariable UUID listingId,
            @RequestParam(required = false) RoomApplication.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        UUID landlordId = SecurityUtils.getCurrentUserId();
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<RoomApplicationResponse> applications = applicationService.getListingApplications(
            listingId, landlordId, status, pageable);
        
        return ResponseEntity.ok(applications);
    }
    
    /**
     * Review application (landlords only)
     */
    @PutMapping("/{applicationId}/review")
    @PreAuthorize("hasRole('LANDLORD')")
    @Operation(summary = "Review application", description = "Accept, reject, or shortlist an application (Landlords only)")
    public ResponseEntity<RoomApplicationResponse> reviewApplication(
            @PathVariable UUID applicationId,
            @Valid @RequestBody ApplicationReviewRequest request) {
        
        UUID landlordId = SecurityUtils.getCurrentUserId();
        log.info("Landlord {} reviewing application {}", landlordId, applicationId);
        
        RoomApplicationResponse response = applicationService.reviewApplication(
            applicationId, landlordId, request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Withdraw application (students only)
     */
    @PutMapping("/{applicationId}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Withdraw application", description = "Cancel/withdraw an application (Students only)")
    public ResponseEntity<RoomApplicationResponse> withdrawApplication(
            @PathVariable UUID applicationId) {
        
        UUID studentId = SecurityUtils.getCurrentUserId();
        log.info("Student {} withdrawing application {}", studentId, applicationId);
        
        RoomApplicationResponse response = applicationService.withdrawApplication(applicationId, studentId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete application
     */
    @DeleteMapping("/{applicationId}")
    @Operation(summary = "Delete application", description = "Delete an application (Student or Landlord)")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID applicationId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} deleting application {}", userId, applicationId);
        
        applicationService.deleteApplication(applicationId, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get the derived progress timeline for an application (tenant or landlord)
     */
    @GetMapping("/{applicationId}/timeline")
    @Operation(summary = "Get application timeline", description = "Derived progress: submitted → reviewing → visit → lease → payment → move-in")
    public ResponseEntity<ApplicationTimelineResponse> getTimeline(@PathVariable UUID applicationId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(applicationTimelineService.buildTimeline(applicationId, userId));
    }

    /**
     * Get application statistics for current user
     */
    @GetMapping("/stats")
    @Operation(summary = "Get application statistics", description = "Get application stats for current user")
    public ResponseEntity<Map<String, Object>> getStats() {
        UUID userId = SecurityUtils.getCurrentUserId();
        String userRole = SecurityUtils.getCurrentUserRole();
        
        Map<String, Object> stats;
        if ("STUDENT".equals(userRole)) {
            stats = applicationService.getStudentStats(userId);
        } else if ("LANDLORD".equals(userRole)) {
            stats = applicationService.getLandlordStats(userId);
        } else {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(stats);
    }
}
