package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.LeaseRequest;
import org.rooms.roombuddy.dto.response.LeaseResponse;
import org.rooms.roombuddy.entity.*;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.LeaseRepository;
import org.rooms.roombuddy.repository.PropertyListingRepository;
import org.rooms.roombuddy.repository.RoomApplicationRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeaseService {
    
    private final LeaseRepository leaseRepository;
    private final RoomApplicationRepository applicationRepository;
    private final PropertyListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    private static final String DEFAULT_TERMS_TEMPLATE = """
        RENTAL AGREEMENT
        
        This agreement is made between the Landlord and the Student (Tenant) for the property listed on RoomBuddy.
        
        TERMS AND CONDITIONS:
        1. The Tenant agrees to pay rent on time each month.
        2. The Tenant agrees to maintain the property in good condition.
        3. The deposit will be refunded within 30 days of move-out, minus any damages.
        4. Either party may terminate with 30 days written notice.
        5. The Tenant agrees to follow all house rules set by the Landlord.
        
        By accepting this lease, both parties agree to these terms.
        """;
    
    public LeaseResponse createLeaseFromApplication(UUID landlordId, LeaseRequest request) {
        log.info("Creating lease from application: {} by landlord: {}", request.getApplicationId(), landlordId);
        
        RoomApplication application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        // Verify landlord owns the listing
        if (!application.getListing().getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("You can only create leases for your own listings");
        }
        
        // Verify application is accepted
        if (application.getStatus() != RoomApplication.Status.ACCEPTED) {
            throw new BadRequestException("Application must be accepted before creating a lease");
        }
        
        // Check if lease already exists for this application
        if (leaseRepository.existsByListingIdAndStudentIdAndStatusIn(
                application.getListing().getId(),
                application.getStudent().getId(),
                Arrays.asList(Lease.LeaseStatus.PENDING_PAYMENT, Lease.LeaseStatus.PENDING_SIGNATURES, Lease.LeaseStatus.ACTIVE))) {
            throw new BadRequestException("An active or pending lease already exists for this student and listing");
        }
        
        PropertyListing listing = application.getListing();
        User student = application.getStudent();
        User landlord = listing.getLandlord();
        
        // Calculate total initial payment
        int deposit = request.getDepositAmount() != null ? request.getDepositAmount() : (listing.getDeposit() != null ? listing.getDeposit() : 0);
        int agencyFees = request.getAgencyFees() != null ? request.getAgencyFees() : (listing.getAgencyFees() != null ? listing.getAgencyFees() : 0);
        int monthlyRent = request.getMonthlyRent() != null ? request.getMonthlyRent() : listing.getRentAmount();
        int totalInitial = deposit + monthlyRent + agencyFees;
        
        Lease lease = Lease.builder()
                .listing(listing)
                .student(student)
                .landlord(landlord)
                .application(application)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .monthlyRent(monthlyRent)
                .depositAmount(deposit)
                .agencyFees(agencyFees)
                .totalInitialPayment(totalInitial)
                .status(Lease.LeaseStatus.PENDING_PAYMENT)
                .termsContent(request.getTermsContent() != null ? request.getTermsContent() : DEFAULT_TERMS_TEMPLATE)
                .build();
        
        Lease saved = leaseRepository.save(lease);
        log.info("Lease created: {} with reference: {}", saved.getId(), saved.getReferenceCode());
        
        // Notify student about lease creation
        notificationService.notifyLeaseCreated(
                student.getId(),
                saved.getId(),
                listing.getTitle()
        );
        
        return mapToResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public LeaseResponse getLeaseById(UUID leaseId, UUID userId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        // Verify user is part of this lease
        if (!lease.getStudent().getId().equals(userId) && !lease.getLandlord().getId().equals(userId)) {
            throw new BadRequestException("You don't have access to this lease");
        }
        
        return mapToResponse(lease);
    }
    
    @Transactional(readOnly = true)
    public LeaseResponse getLeaseByReference(String referenceCode, UUID userId) {
        Lease lease = leaseRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        if (!lease.getStudent().getId().equals(userId) && !lease.getLandlord().getId().equals(userId)) {
            throw new BadRequestException("You don't have access to this lease");
        }
        
        return mapToResponse(lease);
    }
    
    @Transactional(readOnly = true)
    public Page<LeaseResponse> getMyLeases(UUID userId, Pageable pageable) {
        return leaseRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<LeaseResponse> getLeasesAsStudent(UUID studentId, Pageable pageable) {
        return leaseRepository.findByStudentId(studentId, pageable).map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<LeaseResponse> getLeasesAsLandlord(UUID landlordId, Pageable pageable) {
        return leaseRepository.findByLandlordId(landlordId, pageable).map(this::mapToResponse);
    }
    
    public LeaseResponse acceptTerms(UUID leaseId, UUID userId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        if (lease.getStatus() != Lease.LeaseStatus.PENDING_SIGNATURES) {
            throw new BadRequestException("Lease is not ready for signatures. Current status: " + lease.getStatus());
        }
        
        if (lease.getStudent().getId().equals(userId)) {
            lease.setStudentAcceptedTerms(true);
            lease.setStudentAcceptedAt(LocalDateTime.now());
            log.info("Student {} accepted lease terms for lease {}", userId, leaseId);
        } else if (lease.getLandlord().getId().equals(userId)) {
            lease.setLandlordAcceptedTerms(true);
            lease.setLandlordAcceptedAt(LocalDateTime.now());
            log.info("Landlord {} accepted lease terms for lease {}", userId, leaseId);
        } else {
            throw new BadRequestException("You are not part of this lease");
        }
        
        // If both accepted, activate the lease
        if (lease.isBothPartiesAccepted()) {
            lease.setStatus(Lease.LeaseStatus.ACTIVE);
            // Update listing status to RENTED
            PropertyListing listing = lease.getListing();
            listing.setStatus(PropertyListing.Status.RENTED);
            listingRepository.save(listing);
            log.info("Lease {} is now ACTIVE", leaseId);
        }
        
        return mapToResponse(leaseRepository.save(lease));
    }
    
    public LeaseResponse terminateLease(UUID leaseId, UUID userId, Lease.TerminationReason reason, String notes) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        if (!lease.getStudent().getId().equals(userId) && !lease.getLandlord().getId().equals(userId)) {
            throw new BadRequestException("You are not part of this lease");
        }
        
        if (lease.getStatus() != Lease.LeaseStatus.ACTIVE) {
            throw new BadRequestException("Only active leases can be terminated");
        }
        
        lease.setStatus(Lease.LeaseStatus.TERMINATED);
        lease.setTerminationReason(reason);
        lease.setTerminationNotes(notes);
        lease.setTerminatedBy(userId);
        lease.setTerminatedAt(LocalDateTime.now());
        
        // Update listing back to ACTIVE
        PropertyListing listing = lease.getListing();
        listing.setStatus(PropertyListing.Status.ACTIVE);
        listingRepository.save(listing);
        
        log.info("Lease {} terminated by user {} with reason: {}", leaseId, userId, reason);
        
        return mapToResponse(leaseRepository.save(lease));
    }
    
    public LeaseResponse completeLease(UUID leaseId, UUID userId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        if (!lease.getLandlord().getId().equals(userId)) {
            throw new BadRequestException("Only landlord can mark lease as completed");
        }
        
        if (lease.getStatus() != Lease.LeaseStatus.ACTIVE) {
            throw new BadRequestException("Only active leases can be completed");
        }
        
        lease.setStatus(Lease.LeaseStatus.COMPLETED);
        lease.setActualMoveOutDate(java.time.LocalDate.now());
        
        // Update listing back to ACTIVE
        PropertyListing listing = lease.getListing();
        listing.setStatus(PropertyListing.Status.ACTIVE);
        listingRepository.save(listing);
        
        log.info("Lease {} completed", leaseId);
        
        return mapToResponse(leaseRepository.save(lease));
    }
    
    // Called by PaymentService when payment is verified
    public void markPaymentReceived(UUID leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        if (lease.getStatus() == Lease.LeaseStatus.PENDING_PAYMENT) {
            lease.setStatus(Lease.LeaseStatus.PENDING_SIGNATURES);
            leaseRepository.save(lease);
            log.info("Lease {} moved to PENDING_SIGNATURES after payment verification", leaseId);
        }
    }
    
    private LeaseResponse mapToResponse(Lease lease) {
        long months = ChronoUnit.MONTHS.between(lease.getStartDate(), lease.getEndDate());
        
        return LeaseResponse.builder()
                .id(lease.getId())
                .referenceCode(lease.getReferenceCode())
                .listingId(lease.getListing().getId())
                .listingTitle(lease.getListing().getTitle())
                .listingAddress(lease.getListing().getAddress())
                .studentId(lease.getStudent().getId())
                .studentName(lease.getStudent().getFirstName() + " " + lease.getStudent().getLastName())
                .studentEmail(lease.getStudent().getEmail())
                .studentPhone(lease.getStudent().getPhone())
                .landlordId(lease.getLandlord().getId())
                .landlordName(lease.getLandlord().getFirstName() + " " + lease.getLandlord().getLastName())
                .landlordEmail(lease.getLandlord().getEmail())
                .landlordPhone(lease.getLandlord().getPhone())
                .startDate(lease.getStartDate())
                .endDate(lease.getEndDate())
                .monthlyRent(lease.getMonthlyRent())
                .depositAmount(lease.getDepositAmount())
                .agencyFees(lease.getAgencyFees())
                .totalInitialPayment(lease.getTotalInitialPayment())
                .status(lease.getStatus())
                .studentAcceptedTerms(lease.getStudentAcceptedTerms())
                .landlordAcceptedTerms(lease.getLandlordAcceptedTerms())
                .studentAcceptedAt(lease.getStudentAcceptedAt())
                .landlordAcceptedAt(lease.getLandlordAcceptedAt())
                .actualMoveInDate(lease.getActualMoveInDate())
                .actualMoveOutDate(lease.getActualMoveOutDate())
                .terminationReason(lease.getTerminationReason())
                .terminationNotes(lease.getTerminationNotes())
                .terminatedAt(lease.getTerminatedAt())
                .createdAt(lease.getCreatedAt())
                .updatedAt(lease.getUpdatedAt())
                .durationMonths((int) months)
                .canBeReviewed(lease.canBeReviewed())
                .isActive(lease.isActive())
                .build();
    }
    
    /**
     * Create leases for all accepted applications that don't have leases yet
     */
    @Transactional
    public int createLeasesForAcceptedApplications() {
        log.info("Creating leases for accepted applications without leases");
        
        List<RoomApplication> acceptedApplications = applicationRepository.findByStatus(RoomApplication.Status.ACCEPTED);
        int created = 0;
        
        for (RoomApplication application : acceptedApplications) {
            // Check if lease already exists
            boolean leaseExists = leaseRepository.existsByListingIdAndStudentIdAndStatusIn(
                    application.getListing().getId(),
                    application.getStudent().getId(),
                    Arrays.asList(Lease.LeaseStatus.PENDING_PAYMENT, Lease.LeaseStatus.PENDING_SIGNATURES, Lease.LeaseStatus.ACTIVE)
            );
            
            if (!leaseExists) {
                try {
                    PropertyListing listing = application.getListing();
                    User student = application.getStudent();
                    User landlord = listing.getLandlord();
                    
                    int deposit = listing.getDeposit() != null ? listing.getDeposit() : 0;
                    int agencyFees = listing.getAgencyFees() != null ? listing.getAgencyFees() : 0;
                    int monthlyRent = listing.getRentAmount();
                    int totalInitial = deposit + monthlyRent + agencyFees;
                    
                    java.time.LocalDate startDate = application.getMoveInDate() != null ? 
                            application.getMoveInDate() : java.time.LocalDate.now();
                    int leaseDuration = application.getLeaseDurationMonths() != null ? 
                            application.getLeaseDurationMonths() : 12;
                    
                    Lease lease = Lease.builder()
                            .listing(listing)
                            .student(student)
                            .landlord(landlord)
                            .application(application)
                            .startDate(startDate)
                            .endDate(startDate.plusMonths(leaseDuration))
                            .monthlyRent(monthlyRent)
                            .depositAmount(deposit)
                            .agencyFees(agencyFees)
                            .totalInitialPayment(totalInitial)
                            .status(Lease.LeaseStatus.PENDING_PAYMENT)
                            .termsContent(DEFAULT_TERMS_TEMPLATE)
                            .build();
                    
                    leaseRepository.save(lease);
                    created++;
                    log.info("Created lease for application {}", application.getId());
                    
                    // Notify student
                    notificationService.notifyLeaseCreated(
                            student.getId(),
                            lease.getId(),
                            listing.getTitle()
                    );
                } catch (Exception e) {
                    log.warn("Failed to create lease for application {}: {}", application.getId(), e.getMessage());
                }
            }
        }
        
        log.info("Created {} leases for accepted applications", created);
        return created;
    }
}
