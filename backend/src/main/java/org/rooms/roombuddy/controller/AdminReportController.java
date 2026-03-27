package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.ReportResponse;
import org.rooms.roombay.entity.Report;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Reports", description = "Admin APIs for managing reports")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {
    
    private final ReportService reportService;
    
    /**
     * Get all reports with pagination and filters
     */
    @GetMapping
    @Operation(summary = "Get all reports", description = "Get all reports with pagination and filters (Admin only)")
    public ResponseEntity<Page<ReportResponse>> getAllReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        log.info("Admin {} fetching reports - status: {}, entityType: {}, page: {}, size: {}",
                SecurityUtils.getCurrentUserId(), status, entityType, page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ReportResponse> reports;
        
        if (status != null) {
            Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(status.toUpperCase());
            reports = reportService.getReportsByStatus(reportStatus, pageable);
        } else if (entityType != null) {
            Report.EntityType type = Report.EntityType.valueOf(entityType.toUpperCase());
            reports = reportService.getReportsByEntityType(type, pageable);
        } else {
            reports = reportService.getAllReports(pageable);
        }
        
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get urgent/high priority reports
     */
    @GetMapping("/urgent")
    @Operation(summary = "Get urgent reports", description = "Get high priority pending reports (Admin only)")
    public ResponseEntity<List<ReportResponse>> getUrgentReports() {
        log.info("Admin {} fetching urgent reports", SecurityUtils.getCurrentUserId());
        List<ReportResponse> reports = reportService.getUrgentReports();
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get report by ID
     */
    @GetMapping("/{reportId}")
    @Operation(summary = "Get report by ID", description = "Get detailed report information (Admin only)")
    public ResponseEntity<ReportResponse> getReportById(@PathVariable UUID reportId) {
        log.info("Admin {} fetching report: {}", SecurityUtils.getCurrentUserId(), reportId);
        ReportResponse report = reportService.getReportById(reportId);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Resolve a report
     */
    @PostMapping("/{reportId}/resolve")
    @Operation(summary = "Resolve report", description = "Resolve a report with action taken (Admin only)")
    public ResponseEntity<ReportResponse> resolveReport(
            @PathVariable UUID reportId,
            @RequestParam String action,
            @RequestParam(required = false) String notes) {
        
        UUID adminId = SecurityUtils.getCurrentUserId();
        log.info("Admin {} resolving report: {} with action: {}", adminId, reportId, action);
        
        Report.ReportAction reportAction = Report.ReportAction.valueOf(action.toUpperCase());
        ReportResponse resolvedReport = reportService.resolveReport(reportId, adminId, notes, reportAction);
        
        return ResponseEntity.ok(resolvedReport);
    }
    
    /**
     * Dismiss a report
     */
    @PostMapping("/{reportId}/dismiss")
    @Operation(summary = "Dismiss report", description = "Dismiss a report without action (Admin only)")
    public ResponseEntity<ReportResponse> dismissReport(
            @PathVariable UUID reportId,
            @RequestParam(required = false) String reason) {
        
        UUID adminId = SecurityUtils.getCurrentUserId();
        log.info("Admin {} dismissing report: {}", adminId, reportId);
        
        ReportResponse dismissedReport = reportService.dismissReport(reportId, adminId, reason);
        return ResponseEntity.ok(dismissedReport);
    }
    
    /**
     * Update report priority
     */
    @PutMapping("/{reportId}/priority")
    @Operation(summary = "Update report priority", description = "Update the priority of a report (Admin only)")
    public ResponseEntity<ReportResponse> updatePriority(
            @PathVariable UUID reportId,
            @RequestParam String priority) {
        
        log.info("Admin {} updating report priority: {} to {}", 
            SecurityUtils.getCurrentUserId(), reportId, priority);
        
        Report.ReportPriority reportPriority = Report.ReportPriority.valueOf(priority.toUpperCase());
        ReportResponse updatedReport = reportService.updatePriority(reportId, reportPriority);
        
        return ResponseEntity.ok(updatedReport);
    }
    
    /**
     * Get report statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get report statistics", description = "Get comprehensive report statistics (Admin only)")
    public ResponseEntity<Map<String, Object>> getReportStatistics() {
        log.info("Admin {} fetching report statistics", SecurityUtils.getCurrentUserId());
        Map<String, Object> stats = reportService.getReportStatistics();
        return ResponseEntity.ok(stats);
    }
}
