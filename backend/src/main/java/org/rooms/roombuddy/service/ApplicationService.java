package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.ApplicationReviewRequest;
import org.rooms.roombuddy.dto.request.RoomApplicationRequest;
import org.rooms.roombuddy.dto.response.RoomApplicationResponse;
import org.rooms.roombuddy.entity.PropertyListing;
import org.rooms.roombuddy.entity.RoomApplication;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.ListingPhotoRepository;
import org.rooms.roombuddy.repository.ProfileRepository;
import org.rooms.roombuddy.repository.PropertyListingRepository;
import org.rooms.roombuddy.repository.RoomApplicationRepository;
import org.rooms.roombuddy.repository.StudentVerificationRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {
    
    private final RoomApplicationRepository applicationRepository;
    private final PropertyListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ListingPhotoRepository listingPhotoRepository;
    private final StudentVerificationRepository verificationRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;
    
    private static final int APPLICATION_EXPIRY_DAYS = 30;
    
    /**
     * Create a new application
     */
    @Transactional
    public RoomApplicationResponse createApplication(UUID studentId, RoomApplicationRequest request) {
        log.info("Creating application for student {} to listing {}", studentId, request.getListingId());
        
        // Validate student exists and is a STUDENT role
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        
        if (student.getRole() != User.UserRole.STUDENT) {
            throw new BadRequestException("Only students can apply to listings");
        }
        
        // Validate listing exists and is active
        PropertyListing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        
        if (listing.getStatus() != PropertyListing.Status.ACTIVE) {
            throw new BadRequestException("Cannot apply to inactive listings");
        }
        
        // Check if student already applied
        if (applicationRepository.existsByStudentIdAndListingId(studentId, request.getListingId())) {
            throw new BadRequestException("You have already applied to this listing");
        }
        
        // Check if student is trying to apply to their own listing
        if (listing.getLandlord().getId().equals(studentId)) {
            throw new BadRequestException("Cannot apply to your own listing");
        }
        
        // Create application
        RoomApplication application = RoomApplication.builder()
                .listing(listing)
                .student(student)
                .message(request.getMessage())
                .moveInDate(request.getMoveInDate())
                .leaseDurationMonths(request.getLeaseDurationMonths())
                .status(RoomApplication.Status.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(APPLICATION_EXPIRY_DAYS))
                .build();
        
        application = applicationRepository.save(application);
        log.info("Application created successfully with ID: {}", application.getId());
        
        // Send notification to landlord
        notificationService.notifyApplicationReceived(
                listing.getLandlord().getId(),
                application.getId(),
                student.getFirstName() + " " + student.getLastName(),
                listing.getTitle()
        );
        
        return enrichApplicationResponse(RoomApplicationResponse.fromEntity(application));
    }
    
    /**
     * Get application by ID
     */
    @Transactional(readOnly = true)
    public RoomApplicationResponse getApplication(UUID applicationId, UUID userId) {
        RoomApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        // Verify user has permission to view
        validateUserCanViewApplication(application, userId);
        
        // Mark as viewed if landlord is viewing for the first time
        if (application.getListing().getLandlord().getId().equals(userId) &&
            application.getStatus() == RoomApplication.Status.PENDING) {
            application.markAsViewed();
            applicationRepository.save(application);
        }
        
        return enrichApplicationResponse(RoomApplicationResponse.fromEntity(application));
    }
    
    /**
     * Get all applications for a student
     */
    @Transactional(readOnly = true)
    public Page<RoomApplicationResponse> getStudentApplications(
            UUID studentId, 
            RoomApplication.Status status,
            Pageable pageable) {
        
        Page<RoomApplication> applications;
        
        if (status != null) {
            applications = applicationRepository.findByStudentIdAndStatus(studentId, status, pageable);
        } else {
            applications = applicationRepository.findByStudentId(studentId, pageable);
        }
        
        return applications.map(app -> enrichApplicationResponse(RoomApplicationResponse.fromEntity(app)));
    }
    
    /**
     * Get all applications for a landlord's listings
     */
    @Transactional(readOnly = true)
    public Page<RoomApplicationResponse> getLandlordApplications(
            UUID landlordId,
            RoomApplication.Status status,
            Pageable pageable) {
        
        Page<RoomApplication> applications;
        
        if (status != null) {
            applications = applicationRepository.findByLandlordIdAndStatus(landlordId, status, pageable);
        } else {
            applications = applicationRepository.findByLandlordId(landlordId, pageable);
        }
        
        return applications.map(app -> enrichApplicationResponse(RoomApplicationResponse.fromEntity(app)));
    }
    
    /**
     * Get applications for a specific listing
     */
    @Transactional(readOnly = true)
    public Page<RoomApplicationResponse> getListingApplications(
            UUID listingId,
            UUID landlordId,
            RoomApplication.Status status,
            Pageable pageable) {
        
        // Verify landlord owns the listing
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        
        if (!listing.getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("You do not have permission to view these applications");
        }
        
        Page<RoomApplication> applications;
        
        if (status != null) {
            applications = applicationRepository.findByListingIdAndStatus(listingId, status, pageable);
        } else {
            applications = applicationRepository.findByListingId(listingId, pageable);
        }
        
        return applications.map(app -> enrichApplicationResponse(RoomApplicationResponse.fromEntity(app)));
    }
    
    /**
     * Review application (accept/reject/shortlist)
     */
    @Transactional
    public RoomApplicationResponse reviewApplication(
            UUID applicationId,
            UUID landlordId,
            ApplicationReviewRequest request) {
        
        log.info("Landlord {} reviewing application {}", landlordId, applicationId);
        
        RoomApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        // Verify landlord owns the listing
        if (!application.getListing().getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("You do not have permission to review this application");
        }
        
        // Validate status transition
        if (!application.isActive()) {
            throw new BadRequestException("Cannot review application that is already finalized");
        }
        
        // Validate requested status
        if (request.getStatus() != RoomApplication.Status.ACCEPTED &&
            request.getStatus() != RoomApplication.Status.REJECTED &&
            request.getStatus() != RoomApplication.Status.SHORTLISTED) {
            throw new BadRequestException("Invalid status. Must be ACCEPTED, REJECTED, or SHORTLISTED");
        }
        
        // Update application
        application.setStatus(request.getStatus());
        application.setLandlordResponse(request.getResponse());
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(userRepository.findById(landlordId).orElse(null));
        
        if (request.getStatus() == RoomApplication.Status.REJECTED) {
            application.setRejectionReason(request.getRejectionReason());
        }
        
        application = applicationRepository.save(application);
        log.info("Application {} reviewed with status {}", applicationId, request.getStatus());
        
        // Send notification to student based on status
        if (request.getStatus() == RoomApplication.Status.ACCEPTED) {
            notificationService.notifyApplicationAccepted(
                    application.getStudent().getId(),
                    application.getId(),
                    application.getListing().getTitle()
            );
        } else if (request.getStatus() == RoomApplication.Status.REJECTED) {
            notificationService.notifyApplicationRejected(
                    application.getStudent().getId(),
                    application.getId(),
                    application.getListing().getTitle()
            );
        }
        
        return enrichApplicationResponse(RoomApplicationResponse.fromEntity(application));
    }
    
    /**
     * Withdraw application (student cancels)
     */
    @Transactional
    public RoomApplicationResponse withdrawApplication(UUID applicationId, UUID studentId) {
        log.info("Student {} withdrawing application {}", studentId, applicationId);
        
        RoomApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        // Verify student owns the application
        if (!application.getStudent().getId().equals(studentId)) {
            throw new BadRequestException("You do not have permission to withdraw this application");
        }
        
        // Check if can be withdrawn
        if (!application.isActive()) {
            throw new BadRequestException("Cannot withdraw application that is already finalized");
        }
        
        application.setStatus(RoomApplication.Status.WITHDRAWN);
        application = applicationRepository.save(application);
        log.info("Application {} withdrawn successfully", applicationId);
        
        return enrichApplicationResponse(RoomApplicationResponse.fromEntity(application));
    }
    
    /**
     * Delete application
     */
    @Transactional
    public void deleteApplication(UUID applicationId, UUID userId) {
        log.info("User {} deleting application {}", userId, applicationId);
        
        RoomApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        // Only student or landlord can delete
        boolean isStudent = application.getStudent().getId().equals(userId);
        boolean isLandlord = application.getListing().getLandlord().getId().equals(userId);
        
        if (!isStudent && !isLandlord) {
            throw new BadRequestException("You do not have permission to delete this application");
        }
        
        applicationRepository.delete(application);
        log.info("Application {} deleted successfully", applicationId);
    }
    
    /**
     * Get application statistics for a student
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStudentStats(UUID studentId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalApplications", applicationRepository.countByStudentId(studentId));
        stats.put("pendingApplications", applicationRepository.countByStudentIdAndStatus(studentId, RoomApplication.Status.PENDING));
        stats.put("acceptedApplications", applicationRepository.countByStudentIdAndStatus(studentId, RoomApplication.Status.ACCEPTED));
        stats.put("rejectedApplications", applicationRepository.countByStudentIdAndStatus(studentId, RoomApplication.Status.REJECTED));
        
        return stats;
    }
    
    /**
     * Get application statistics for a landlord
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLandlordStats(UUID landlordId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalApplications", applicationRepository.countByLandlordId(landlordId));
        stats.put("pendingApplications", applicationRepository.countByLandlordIdAndStatus(landlordId, RoomApplication.Status.PENDING));
        stats.put("acceptedApplications", applicationRepository.countByLandlordIdAndStatus(landlordId, RoomApplication.Status.ACCEPTED));
        stats.put("rejectedApplications", applicationRepository.countByLandlordIdAndStatus(landlordId, RoomApplication.Status.REJECTED));
        
        return stats;
    }
    
    /**
     * Process expired applications (scheduled task)
     */
    @Transactional
    public int processExpiredApplications() {
        List<RoomApplication> expiredApplications = applicationRepository.findExpiredApplications(LocalDateTime.now());
        
        for (RoomApplication app : expiredApplications) {
            app.setStatus(RoomApplication.Status.EXPIRED);
        }
        
        applicationRepository.saveAll(expiredApplications);
        log.info("Processed {} expired applications", expiredApplications.size());
        
        return expiredApplications.size();
    }
    
    /**
     * Enrich application response with photos and verification data
     */
    private RoomApplicationResponse enrichApplicationResponse(RoomApplicationResponse response) {
        if (response == null) {
            return null;
        }
        
        // Get listing primary photo
        if (response.getListingId() != null) {
            listingPhotoRepository.findByListingIdAndIsPrimary(response.getListingId(), true)
                .ifPresent(photo -> response.setListingPrimaryPhotoUrl(photo.getPhotoUrl()));
            
            // If no primary photo, get the first one
            if (response.getListingPrimaryPhotoUrl() == null) {
                listingPhotoRepository.findFirstByListingIdOrderByDisplayOrderAsc(response.getListingId())
                    .ifPresent(photo -> response.setListingPrimaryPhotoUrl(photo.getPhotoUrl()));
            }
        }
        
        // Get student verification status
        if (response.getStudentId() != null) {
            verificationRepository.findByUserId(response.getStudentId())
                .ifPresent(verification -> 
                    response.setStudentVerified(verification.getStatus() == org.rooms.roombuddy.entity.StudentVerification.Status.VERIFIED)
                );
            
            // Get student profile photo
            profileRepository.findByUserId(response.getStudentId())
                .ifPresent(profile -> response.setStudentProfilePhotoUrl(profile.getProfilePhotoUrl()));
        }
        
        return response;
    }
    
    /**
     * Validate user can view application
     */
    private void validateUserCanViewApplication(RoomApplication application, UUID userId) {
        boolean isStudent = application.getStudent().getId().equals(userId);
        boolean isLandlord = application.getListing().getLandlord().getId().equals(userId);
        
        if (!isStudent && !isLandlord) {
            throw new BadRequestException("You do not have permission to view this application");
        }
    }
}
