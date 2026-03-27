package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.DisputeRequest;
import org.rooms.roombay.dto.response.DisputeResponse;
import org.rooms.roombay.entity.Dispute;
import org.rooms.roombay.entity.Lease;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.DisputeRepository;
import org.rooms.roombay.repository.LeaseRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DisputeService {
    
    private final DisputeRepository disputeRepository;
    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public DisputeResponse createDispute(UUID userId, DisputeRequest request) {
        log.info("Creating dispute for lease: {} by user: {}", request.getLeaseId(), userId);
        
        Lease lease = leaseRepository.findById(request.getLeaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        User openedBy = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify user is part of the lease
        boolean isStudent = lease.getStudent().getId().equals(userId);
        boolean isLandlord = lease.getLandlord().getId().equals(userId);
        
        if (!isStudent && !isLandlord) {
            throw new BadRequestException("You can only open disputes for leases you are part of");
        }
        
        // Check for existing open dispute
        if (disputeRepository.existsByLeaseIdAndStatusNotIn(lease.getId(),
                Arrays.asList(Dispute.DisputeStatus.RESOLVED, Dispute.DisputeStatus.CLOSED))) {
            throw new BadRequestException("There is already an open dispute for this lease");
        }
        
        User againstUser = isStudent ? lease.getLandlord() : lease.getStudent();
        
        // Determine priority based on category
        Dispute.DisputePriority priority = determinePriority(request.getCategory());
        
        String evidenceUrls = request.getEvidenceUrls() != null ? 
                String.join(",", request.getEvidenceUrls()) : null;
        
        Dispute dispute = Dispute.builder()
                .lease(lease)
                .openedBy(openedBy)
                .againstUser(againstUser)
                .category(request.getCategory())
                .title(request.getTitle())
                .description(request.getDescription())
                .evidenceUrls(evidenceUrls)
                .status(Dispute.DisputeStatus.OPEN)
                .priority(priority)
                .build();
        
        // Update lease status if active
        if (lease.getStatus() == Lease.LeaseStatus.ACTIVE) {
            lease.setStatus(Lease.LeaseStatus.DISPUTED);
            leaseRepository.save(lease);
        }
        
        Dispute saved = disputeRepository.save(dispute);
        log.info("Dispute created: {} with reference: {}", saved.getId(), saved.getReferenceCode());
        
        return mapToResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public DisputeResponse getDisputeById(UUID disputeId, UUID userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        
        // Verify user has access
        if (!dispute.getOpenedBy().getId().equals(userId) && 
            !dispute.getAgainstUser().getId().equals(userId) &&
            (dispute.getAssignedTo() == null || !dispute.getAssignedTo().getId().equals(userId))) {
            throw new BadRequestException("You don't have access to this dispute");
        }
        
        return mapToResponse(dispute);
    }
    
    @Transactional(readOnly = true)
    public Page<DisputeResponse> getMyDisputes(UUID userId, Pageable pageable) {
        return disputeRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }
    
    public DisputeResponse addEvidence(UUID disputeId, UUID userId, List<String> evidenceUrls) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        
        if (!dispute.getOpenedBy().getId().equals(userId) && !dispute.getAgainstUser().getId().equals(userId)) {
            throw new BadRequestException("You can only add evidence to your own disputes");
        }
        
        String existing = dispute.getEvidenceUrls();
        String newEvidence = String.join(",", evidenceUrls);
        dispute.setEvidenceUrls(existing != null ? existing + "," + newEvidence : newEvidence);
        
        return mapToResponse(disputeRepository.save(dispute));
    }
    
    // Admin methods
    @Transactional(readOnly = true)
    public Page<DisputeResponse> getActiveDisputes(Pageable pageable) {
        return disputeRepository.findActiveDisputes(pageable).map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<DisputeResponse> getDisputesByStatus(Dispute.DisputeStatus status, Pageable pageable) {
        return disputeRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public List<DisputeResponse> getUrgentDisputes() {
        return disputeRepository.findUrgentDisputes().stream().map(this::mapToResponse).toList();
    }
    
    public DisputeResponse assignDispute(UUID disputeId, UUID adminId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        
        dispute.setAssignedTo(admin);
        dispute.setAssignedAt(LocalDateTime.now());
        dispute.setStatus(Dispute.DisputeStatus.UNDER_REVIEW);
        
        log.info("Dispute {} assigned to admin {}", disputeId, adminId);
        
        return mapToResponse(disputeRepository.save(dispute));
    }
    
    public DisputeResponse updateDisputeStatus(UUID disputeId, UUID adminId, Dispute.DisputeStatus status) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        
        dispute.setStatus(status);
        log.info("Dispute {} status updated to {} by admin {}", disputeId, status, adminId);
        
        Dispute saved = disputeRepository.save(dispute);
        
        // Notify both parties about status update
        notificationService.notifyDisputeUpdated(
                dispute.getOpenedBy().getId(),
                dispute.getId(),
                status.name()
        );
        notificationService.notifyDisputeUpdated(
                dispute.getAgainstUser().getId(),
                dispute.getId(),
                status.name()
        );
        
        return mapToResponse(saved);
    }
    
    public DisputeResponse resolveDispute(UUID disputeId, UUID adminId, Dispute.DisputeOutcome outcome, 
                                          String notes, Integer refundAmount) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        
        dispute.setStatus(Dispute.DisputeStatus.RESOLVED);
        dispute.setOutcome(outcome);
        dispute.setResolutionNotes(notes);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolvedBy(admin);
        
        if (refundAmount != null && refundAmount > 0) {
            dispute.setRefundAmount(refundAmount);
            dispute.setRefundApproved(true);
        }
        
        // Update lease status back to active if it was disputed
        Lease lease = dispute.getLease();
        if (lease.getStatus() == Lease.LeaseStatus.DISPUTED) {
            lease.setStatus(Lease.LeaseStatus.ACTIVE);
            leaseRepository.save(lease);
        }
        
        log.info("Dispute {} resolved by admin {} with outcome: {}", disputeId, adminId, outcome);
        
        Dispute saved = disputeRepository.save(dispute);
        
        // Notify both parties about resolution
        notificationService.notifyDisputeResolved(
                dispute.getOpenedBy().getId(),
                dispute.getId(),
                outcome.name()
        );
        notificationService.notifyDisputeResolved(
                dispute.getAgainstUser().getId(),
                dispute.getId(),
                outcome.name()
        );
        
        return mapToResponse(saved);
    }
    
    private Dispute.DisputePriority determinePriority(Dispute.DisputeCategory category) {
        return switch (category) {
            case HARASSMENT -> Dispute.DisputePriority.URGENT;
            case PAYMENT_ISSUE, DEPOSIT_REFUND -> Dispute.DisputePriority.HIGH;
            case PROPERTY_CONDITION, LEASE_VIOLATION, MISREPRESENTATION -> Dispute.DisputePriority.MEDIUM;
            default -> Dispute.DisputePriority.LOW;
        };
    }
    
    private DisputeResponse mapToResponse(Dispute dispute) {
        List<String> evidenceList = dispute.getEvidenceUrls() != null ?
                Arrays.asList(dispute.getEvidenceUrls().split(",")) : null;
        
        return DisputeResponse.builder()
                .id(dispute.getId())
                .referenceCode(dispute.getReferenceCode())
                .leaseId(dispute.getLease().getId())
                .leaseReferenceCode(dispute.getLease().getReferenceCode())
                .openedById(dispute.getOpenedBy().getId())
                .openedByName(dispute.getOpenedBy().getFirstName() + " " + dispute.getOpenedBy().getLastName())
                .againstUserId(dispute.getAgainstUser().getId())
                .againstUserName(dispute.getAgainstUser().getFirstName() + " " + dispute.getAgainstUser().getLastName())
                .category(dispute.getCategory())
                .title(dispute.getTitle())
                .description(dispute.getDescription())
                .evidenceUrls(evidenceList)
                .status(dispute.getStatus())
                .priority(dispute.getPriority())
                .assignedToId(dispute.getAssignedTo() != null ? dispute.getAssignedTo().getId() : null)
                .assignedToName(dispute.getAssignedTo() != null ? 
                        dispute.getAssignedTo().getFirstName() + " " + dispute.getAssignedTo().getLastName() : null)
                .assignedAt(dispute.getAssignedAt())
                .outcome(dispute.getOutcome())
                .resolutionNotes(dispute.getResolutionNotes())
                .resolvedAt(dispute.getResolvedAt())
                .resolvedByName(dispute.getResolvedBy() != null ?
                        dispute.getResolvedBy().getFirstName() + " " + dispute.getResolvedBy().getLastName() : null)
                .refundAmount(dispute.getRefundAmount())
                .refundApproved(dispute.getRefundApproved())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .build();
    }
}
