package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Admin APIs for platform analytics")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    /**
     * Get complete dashboard analytics
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard analytics", description = "Get comprehensive dashboard analytics (Admin only)")
    public ResponseEntity<Map<String, Object>> getDashboardAnalytics() {
        log.info("Admin {} fetching dashboard analytics", SecurityUtils.getCurrentUserId());
        Map<String, Object> analytics = analyticsService.getDashboardAnalytics();
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get overview analytics
     */
    @GetMapping("/overview")
    @Operation(summary = "Get overview analytics", description = "Get platform overview metrics (Admin only)")
    public ResponseEntity<Map<String, Object>> getOverviewAnalytics() {
        log.info("Admin {} fetching overview analytics", SecurityUtils.getCurrentUserId());
        Map<String, Object> analytics = analyticsService.getOverviewAnalytics();
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get user growth analytics
     */
    @GetMapping("/users")
    @Operation(summary = "Get user analytics", description = "Get user growth and activity metrics (Admin only)")
    public ResponseEntity<Map<String, Object>> getUserGrowthAnalytics() {
        log.info("Admin {} fetching user growth analytics", SecurityUtils.getCurrentUserId());
        Map<String, Object> analytics = analyticsService.getUserGrowthAnalytics();
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get listing analytics
     */
    @GetMapping("/listings")
    @Operation(summary = "Get listing analytics", description = "Get listing metrics and trends (Admin only)")
    public ResponseEntity<Map<String, Object>> getListingAnalytics() {
        log.info("Admin {} fetching listing analytics", SecurityUtils.getCurrentUserId());
        Map<String, Object> analytics = analyticsService.getListingAnalytics();
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get engagement analytics
     */
    @GetMapping("/engagement")
    @Operation(summary = "Get engagement analytics", description = "Get user engagement metrics (Admin only)")
    public ResponseEntity<Map<String, Object>> getEngagementAnalytics() {
        log.info("Admin {} fetching engagement analytics", SecurityUtils.getCurrentUserId());
        Map<String, Object> analytics = analyticsService.getEngagementAnalytics();
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get conversion analytics
     */
    @GetMapping("/conversions")
    @Operation(summary = "Get conversion analytics", description = "Get conversion rate metrics (Admin only)")
    public ResponseEntity<Map<String, Object>> getConversionAnalytics() {
        log.info("Admin {} fetching conversion analytics", SecurityUtils.getCurrentUserId());
        Map<String, Object> analytics = analyticsService.getConversionAnalytics();
        return ResponseEntity.ok(analytics);
    }
}
