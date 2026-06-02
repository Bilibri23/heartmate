package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.ApplicationReviewRequest;
import org.rooms.roombay.dto.request.RoomApplicationRequest;
import org.rooms.roombay.dto.response.RoomApplicationResponse;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.CoApplicationInvitationRepository;
import org.rooms.roombay.repository.ListingPhotoRepository;
import org.rooms.roombay.repository.MatchRepository;
import org.rooms.roombay.repository.ProfileRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.rooms.roombay.repository.UserRepository;
import org.rooms.roombay.entity.CoApplicationInvitation;
import org.rooms.roombay.entity.Match;
import org.rooms.roombay.entity.Notification;
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
    private final CoApplicationInvitationRepository coInvitationRepository;
    private final MatchRepository matchRepository;
    private final org.springframework.context.ApplicationContext applicationContext;
    private final AnalyticsEventService analyticsEventService;
    
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
        analyticsEventService.emit("application_started", studentId, "STUDENT", listing.getId(),
                Map.of("applicationId", application.getId().toString()));
        analyticsEventService.emit("application_submitted", studentId, "STUDENT", listing.getId(),
                Map.of("applicationId", application.getId().toString()));
        
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
     * Create a co-application (joint application) from an accepted invitation
     */
    @Transactional
    public RoomApplicationResponse createCoApplication(UUID invitationId, UUID applicantId, RoomApplicationRequest request) {
        log.info("Creating co-application from invitation {} by user {}", invitationId, applicantId);
        
        // Get the accepted invitation
        CoApplicationInvitation invitation = coInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        
        // Validate invitation is accepted
        if (invitation.getStatus() != CoApplicationInvitation.Status.ACCEPTED) {
            throw new BadRequestException("Invitation must be accepted before applying together");
        }
        
        // Validate user is part of the invitation
        UUID inviterId = invitation.getInviter().getId();
        UUID inviteeId = invitation.getInvitee().getId();
        
        if (!applicantId.equals(inviterId) && !applicantId.equals(inviteeId)) {
            throw new BadRequestException("You are not part of this invitation");
        }
        
        // Determine who is the primary applicant and who is the co-applicant
        User primaryApplicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UUID coApplicantId = applicantId.equals(inviterId) ? inviteeId : inviterId;
        User coApplicant = userRepository.findById(coApplicantId)
                .orElseThrow(() -> new ResourceNotFoundException("Co-applicant not found"));
        
        PropertyListing listing = invitation.getListing();
        Match match = invitation.getMatch();
        
        // Validate listing is still active
        if (listing.getStatus() != PropertyListing.Status.ACTIVE) {
            throw new BadRequestException("This listing is no longer available");
        }
        
        // Check if either user already has an application for this listing
        if (applicationRepository.existsByStudentIdAndListingId(applicantId, listing.getId())) {
            throw new BadRequestException("You have already applied to this listing");
        }
        if (applicationRepository.existsByStudentIdAndListingId(coApplicantId, listing.getId())) {
            throw new BadRequestException("Your roommate has already applied to this listing");
        }
        
        // Create co-application
        RoomApplication application = RoomApplication.builder()
                .listing(listing)
                .student(primaryApplicant)
                .coApplicant(coApplicant)
                .match(match)
                .isCoApplication(true)
                .coApplicantConfirmed(false)
                .message(request.getMessage())
                .moveInDate(request.getMoveInDate())
                .leaseDurationMonths(request.getLeaseDurationMonths())
                .status(RoomApplication.Status.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(APPLICATION_EXPIRY_DAYS))
                .build();
        
        application = applicationRepository.save(application);
        log.info("Co-application created successfully with ID: {}", application.getId());
        analyticsEventService.emit("application_started", applicantId, primaryApplicant.getRole().name(), listing.getId(),
                Map.of("applicationId", application.getId().toString(), "coApplication", true));
        analyticsEventService.emit("application_submitted", applicantId, primaryApplicant.getRole().name(), listing.getId(),
                Map.of("applicationId", application.getId().toString(), "coApplication", true));
        
        // Mark the invitation as used (change status to reflect it's been acted upon)
        invitation.setStatus(CoApplicationInvitation.Status.APPLIED);
        coInvitationRepository.save(invitation);
        
        // Notify co-applicant to confirm
        notificationService.createNotification(
                coApplicantId,
                Notification.NotificationType.APPLICATION_RECEIVED,
                "Confirm Co-Application",
                primaryApplicant.getFirstName() + " submitted a joint application for " + listing.getTitle() + ". Please confirm.",
                application.getId(),
                "CO_APPLICATION",
                "/applications/" + application.getId()
        );
        
        // Notify landlord
        String applicantsName = primaryApplicant.getFirstName() + " & " + coApplicant.getFirstName();
        notificationService.createNotification(
                listing.getLandlord().getId(),
                Notification.NotificationType.APPLICATION_RECEIVED,
                "New Joint Application",
                applicantsName + " applied together for " + listing.getTitle(),
                application.getId(),
                "APPLICATION",
                "/landlord/applications/" + application.getId()
        );
        
        return enrichApplicationResponse(RoomApplicationResponse.fromEntity(application));
    }
    
    /**
     * Confirm co-application (by co-applicant)
     */
    @Transactional
    public RoomApplicationResponse confirmCoApplication(UUID applicationId, UUID coApplicantId) {
        log.info("Co-applicant {} confirming application {}", coApplicantId, applicationId);
        
        RoomApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        if (!application.getIsCoApplication()) {
            throw new BadRequestException("This is not a co-application");
        }
        
        if (application.getCoApplicant() == null || !application.getCoApplicant().getId().equals(coApplicantId)) {
            throw new BadRequestException("You are not the co-applicant for this application");
        }
        
        if (application.getCoApplicantConfirmed()) {
            throw new BadRequestException("You have already confirmed this application");
        }
        
        application.setCoApplicantConfirmed(true);
        application.setCoApplicantConfirmedAt(LocalDateTime.now());
        application = applicationRepository.save(application);
        
        // Notify primary applicant
        notificationService.createNotification(
                application.getStudent().getId(),
                Notification.NotificationType.APPLICATION_RECEIVED,
                "Co-Application Confirmed",
                application.getCoApplicant().getFirstName() + " confirmed your joint application!",
                application.getId(),
                "APPLICATION",
                "/applications/" + application.getId()
        );
        
        log.info("Co-application {} confirmed by co-applicant", applicationId);
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
     * Get current student's application for a listing
     */
    @Transactional(readOnly = true)
    public RoomApplicationResponse getStudentApplicationForListing(UUID studentId, UUID listingId) {
        RoomApplication application = applicationRepository.findByStudentIdAndListingId(studentId, listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

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
            analyticsEventService.emit("application_approved", landlordId, "LANDLORD", application.getListing().getId(),
                    Map.of("applicationId", application.getId().toString()));
            notificationService.notifyApplicationAccepted(
                    application.getStudent().getId(),
                    application.getId(),
                    application.getListing().getTitle()
            );
            
            // Auto-create lease when application is accepted
            try {
                LeaseService leaseService = applicationContext.getBean(LeaseService.class);
                org.rooms.roombay.dto.request.LeaseRequest leaseRequest = new org.rooms.roombay.dto.request.LeaseRequest();
                leaseRequest.setApplicationId(application.getId());
                leaseRequest.setStartDate(application.getMoveInDate());
                leaseRequest.setEndDate(application.getMoveInDate().plusMonths(application.getLeaseDurationMonths()));
                leaseRequest.setMonthlyRent(application.getListing().getRentAmount());
                leaseRequest.setDepositAmount(application.getListing().getDeposit());
                leaseService.createLeaseFromApplication(landlordId, leaseRequest);
                log.info("Auto-created lease for accepted application {}", applicationId);
            } catch (Exception e) {
                log.warn("Failed to auto-create lease for application {}: {}", applicationId, e.getMessage());
            }
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
                    response.setStudentVerified(verification.getStatus() == org.rooms.roombay.entity.StudentVerification.Status.VERIFIED)
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
