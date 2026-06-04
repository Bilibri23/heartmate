package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.ReportRequest;
import org.rooms.roombay.dto.response.ReportResponse;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.Report;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.ReportRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.data.domain.Page;
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
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PropertyListingRepository listingRepository;
    private final AuditLogService auditLogService;
    
    /**
     * Create a new report
     */
    @Transactional
    public ReportResponse createReport(UUID reporterId, ReportRequest request) {
        log.info("Creating report from user: {} for entity: {} ({})", 
            reporterId, request.getEntityId(), request.getEntityType());
        
        // Validate reporter exists
        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new ResourceNotFoundException("Reporter not found"));
        
        // Check if user already reported this entity
        boolean alreadyReported = reportRepository.existsByReporterIdAndReportedEntityTypeAndReportedEntityIdAndStatus(
            reporterId,
            request.getEntityType(),
            request.getEntityId(),
            Report.ReportStatus.PENDING
        );
        
        if (alreadyReported) {
            throw new BadRequestException("You have already reported this " + request.getEntityType().name().toLowerCase());
        }
        
        // Validate reported entity exists
        validateEntityExists(request.getEntityType(), request.getEntityId());
        
        // Determine priority based on reason
        Report.ReportPriority priority = determinePriority(request.getReason());
        
        // Create report
        Report report = Report.builder()
            .reporterId(reporterId)
            .reportedEntityType(request.getEntityType())
            .reportedEntityId(request.getEntityId())
            .reason(request.getReason())
            .description(request.getDescription())
            .status(Report.ReportStatus.PENDING)
            .priority(priority)
            .build();
        
        Report savedReport = reportRepository.save(report);
        log.info("Report created successfully: {}", savedReport.getId());
        
        return mapToReportResponse(savedReport);
    }
    
    /**
     * Get all reports with pagination
     */
    public Page<ReportResponse> getAllReports(Pageable pageable) {
        log.info("Fetching all reports with pagination: {}", pageable);
        return reportRepository.findAll(pageable)
            .map(this::mapToReportResponse);
    }
    
    /**
     * Get reports by status
     */
    public Page<ReportResponse> getReportsByStatus(Report.ReportStatus status, Pageable pageable) {
        log.info("Fetching reports by status: {}", status);
        return reportRepository.findByStatus(status, pageable)
            .map(this::mapToReportResponse);
    }
    
    /**
     * Get reports by entity type
     */
    public Page<ReportResponse> getReportsByEntityType(Report.EntityType entityType, Pageable pageable) {
        log.info("Fetching reports by entity type: {}", entityType);
        return reportRepository.findByReportedEntityType(entityType, pageable)
            .map(this::mapToReportResponse);
    }
    
    /**
     * Get reports by reporter
     */
    public Page<ReportResponse> getReportsByReporter(UUID reporterId, Pageable pageable) {
        log.info("Fetching reports by reporter: {}", reporterId);
        return reportRepository.findByReporterId(reporterId, pageable)
            .map(this::mapToReportResponse);
    }
    
    /**
     * Get urgent/high priority reports
     */
    public List<ReportResponse> getUrgentReports() {
        log.info("Fetching urgent reports");
        return reportRepository.findUrgentReports().stream()
            .map(this::mapToReportResponse)
            .toList();
    }
    
    /**
     * Get report by ID
     */
    public ReportResponse getReportById(UUID reportId) {
        log.info("Fetching report by ID: {}", reportId);
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));
        return mapToReportResponse(report);
    }
    
    /**
     * Review and resolve a report
     */
    @Transactional
    public ReportResponse resolveReport(UUID reportId, UUID adminId, String resolutionNotes, Report.ReportAction action) {
        log.info("Resolving report: {} by admin: {}", reportId, adminId);
        
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));
        
        if (report.getStatus() == Report.ReportStatus.RESOLVED) {
            throw new BadRequestException("Report is already resolved");
        }
        
        report.setStatus(Report.ReportStatus.RESOLVED);
        report.setReviewedBy(adminId);
        report.setReviewedAt(LocalDateTime.now());
        report.setResolutionNotes(resolutionNotes);
        report.setActionTaken(action);
        
        Report resolvedReport = reportRepository.save(report);
        log.info("Report resolved successfully: {}", reportId);
        auditLogService.logAdminAction(
                adminId,
                "REPORT_RESOLVED",
                "Report",
                reportId,
                "Report resolved. action=" + action + ", notes=" + resolutionNotes
        );
        
        // Execute action if needed
        executeAction(report, action);
        
        return mapToReportResponse(resolvedReport);
    }
    
    /**
     * Dismiss a report
     */
    @Transactional
    public ReportResponse dismissReport(UUID reportId, UUID adminId, String reason) {
        log.info("Dismissing report: {} by admin: {}", reportId, adminId);
        
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));
        
        report.setStatus(Report.ReportStatus.DISMISSED);
        report.setReviewedBy(adminId);
        report.setReviewedAt(LocalDateTime.now());
        report.setResolutionNotes(reason);
        report.setActionTaken(Report.ReportAction.NO_ACTION);
        
        Report dismissedReport = reportRepository.save(report);
        log.info("Report dismissed successfully: {}", reportId);
        auditLogService.logAdminAction(
                adminId,
                "REPORT_DISMISSED",
                "Report",
                reportId,
                "Report dismissed. reason=" + reason
        );
        
        return mapToReportResponse(dismissedReport);
    }
    
    /**
     * Update report priority
     */
    @Transactional
    public ReportResponse updatePriority(UUID reportId, Report.ReportPriority priority) {
        log.info("Updating report priority: {} to {}", reportId, priority);
        
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));
        
        report.setPriority(priority);
        Report updatedReport = reportRepository.save(report);
        
        return mapToReportResponse(updatedReport);
    }
    
    /**
     * Get report statistics
     */
    public Map<String, Object> getReportStatistics() {
        log.info("Fetching report statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        // Stats by status
        Map<String, Long> statusStats = new HashMap<>();
        for (Object[] row : reportRepository.getReportStatsByStatus()) {
            Report.ReportStatus status = (Report.ReportStatus) row[0];
            Long count = (Long) row[1];
            statusStats.put(status.name(), count);
        }
        stats.put("byStatus", statusStats);
        
        // Stats by reason
        Map<String, Long> reasonStats = new HashMap<>();
        for (Object[] row : reportRepository.getReportStatsByReason()) {
            Report.ReportReason reason = (Report.ReportReason) row[0];
            Long count = (Long) row[1];
            reasonStats.put(reason.name(), count);
        }
        stats.put("byReason", reasonStats);
        
        // Stats by priority for pending reports
        Map<String, Long> priorityStats = new HashMap<>();
        for (Object[] row : reportRepository.getPendingReportsByPriority(Report.ReportStatus.PENDING)) {
            Report.ReportPriority priority = (Report.ReportPriority) row[0];
            Long count = (Long) row[1];
            priorityStats.put(priority.name(), count);
        }
        stats.put("pendingByPriority", priorityStats);
        
        // Total counts
        stats.put("totalReports", reportRepository.count());
        stats.put("pendingReports", reportRepository.countByStatus(Report.ReportStatus.PENDING));
        stats.put("resolvedReports", reportRepository.countByStatus(Report.ReportStatus.RESOLVED));
        stats.put("urgentReports", (long) reportRepository.findUrgentReports().size());
        
        return stats;
    }
    
    /**
     * Validate that reported entity exists
     */
    private void validateEntityExists(Report.EntityType entityType, UUID entityId) {
        switch (entityType) {
            case USER:
                if (!userRepository.existsById(entityId)) {
                    throw new ResourceNotFoundException("Reported user not found");
                }
                break;
            case LISTING:
                if (!listingRepository.existsById(entityId)) {
                    throw new ResourceNotFoundException("Reported listing not found");
                }
                break;
        }
    }
    
    /**
     * Determine priority based on reason
     */
    private Report.ReportPriority determinePriority(Report.ReportReason reason) {
        return switch (reason) {
            case SCAM, FRAUD -> Report.ReportPriority.CRITICAL;
            case HARASSMENT, INAPPROPRIATE -> Report.ReportPriority.HIGH;
            case SPAM, MISLEADING -> Report.ReportPriority.MEDIUM;
            default -> Report.ReportPriority.LOW;
        };
    }
    
    /**
     * Execute action on reported entity
     */
    private void executeAction(Report report, Report.ReportAction action) {
        if (action == null || action == Report.ReportAction.NO_ACTION) {
            return;
        }
        
        log.info("Executing action: {} on entity: {}", action, report.getReportedEntityId());
        
        switch (action) {
            case LISTING_DELETED:
                if (report.getReportedEntityType() == Report.EntityType.LISTING) {
                    // Set listing status to DELETED
                    listingRepository.findById(report.getReportedEntityId()).ifPresent(listing -> {
                        listing.setStatus(PropertyListing.Status.DELETED);
                        listingRepository.save(listing);
                        log.info("Listing deleted: {}", report.getReportedEntityId());
                    });
                }
                break;
            case SUSPENDED, BANNED:
                if (report.getReportedEntityType() == Report.EntityType.LISTING) {
                    userRepository.findById(report.getReportedEntityId()).ifPresent(user -> {
                        user.setSuspended(true);
                        user.setSuspendedAt(LocalDateTime.now());
                        user.setSuspendedBy(report.getReviewedBy());
                        user.setSuspensionReason("Reported: " + report.getReason());
                        user.setAccountStatus(User.AccountStatus.SUSPENDED);
                        userRepository.save(user);
                        log.info("User suspended: {}", report.getReportedEntityId());
                    });
                }
                break;
        }
    }
    
    /**
     * Map Report entity to ReportResponse DTO
     */
    private ReportResponse mapToReportResponse(Report report) {
        // Get reporter info
        String reporterName = null;
        String reporterEmail = null;
        userRepository.findById(report.getReporterId()).ifPresent(user -> {
            // Using a trick to set these in the scope
        });
        
        User reporter = userRepository.findById(report.getReporterId()).orElse(null);
        if (reporter != null) {
            reporterName = reporter.getFirstName() + " " + reporter.getLastName();
            reporterEmail = reporter.getEmail();
        }
        
        // Get reported entity name
        String reportedEntityName = null;
        if (report.getReportedEntityType() == Report.EntityType.USER) {
            User reportedUser = userRepository.findById(report.getReportedEntityId()).orElse(null);
            if (reportedUser != null) {
                reportedEntityName = reportedUser.getFirstName() + " " + reportedUser.getLastName();
            }
        } else if (report.getReportedEntityType() == Report.EntityType.LISTING) {
            PropertyListing listing = listingRepository.findById(report.getReportedEntityId()).orElse(null);
            if (listing != null) {
                reportedEntityName = listing.getTitle();
            }
        }
        
        // Get reviewer info
        String reviewedByName = null;
        if (report.getReviewedBy() != null) {
            User reviewer = userRepository.findById(report.getReviewedBy()).orElse(null);
            if (reviewer != null) {
                reviewedByName = reviewer.getFirstName() + " " + reviewer.getLastName();
            }
        }
        
        return ReportResponse.builder()
            .id(report.getId())
            .reporterId(report.getReporterId())
            .reporterName(reporterName)
            .reporterEmail(reporterEmail)
            .reportedEntityType(report.getReportedEntityType())
            .reportedEntityId(report.getReportedEntityId())
            .reportedEntityName(reportedEntityName)
            .reason(report.getReason())
            .description(report.getDescription())
            .status(report.getStatus())
            .priority(report.getPriority())
            .reviewedBy(report.getReviewedBy())
            .reviewedByName(reviewedByName)
            .reviewedAt(report.getReviewedAt())
            .resolutionNotes(report.getResolutionNotes())
            .actionTaken(report.getActionTaken())
            .createdAt(report.getCreatedAt())
            .updatedAt(report.getUpdatedAt())
            .build();
    }
}
