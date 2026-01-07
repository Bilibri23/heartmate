package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.ReportRequest;
import org.rooms.roombuddy.dto.response.ReportResponse;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "APIs for reporting users and listings")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {
    
    private final ReportService reportService;
    
    /**
     * Create a new report (User-facing)
     */
    @PostMapping
    @Operation(summary = "Create report", description = "Report a user or listing")
    public ResponseEntity<ReportResponse> createReport(@Valid @RequestBody ReportRequest request) {
        UUID reporterId = SecurityUtils.getCurrentUserId();
        log.info("User {} creating report for entity: {} ({})", 
            reporterId, request.getEntityId(), request.getEntityType());
        
        ReportResponse report = reportService.createReport(reporterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }
    
    /**
     * Get my reports (User-facing)
     */
    @GetMapping("/my-reports")
    @Operation(summary = "Get my reports", description = "Get all reports created by the current user")
    public ResponseEntity<Page<ReportResponse>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID reporterId = SecurityUtils.getCurrentUserId();
        log.info("User {} fetching their reports", reporterId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> reports = reportService.getReportsByReporter(reporterId, pageable);
        
        return ResponseEntity.ok(reports);
    }
}
