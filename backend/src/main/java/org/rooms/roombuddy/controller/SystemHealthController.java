package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.SystemHealthService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System Health", description = "Admin APIs for system health monitoring")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class SystemHealthController {
    
    private final SystemHealthService systemHealthService;
    
    /**
     * Get system health status
     */
    @GetMapping("/health")
    @Operation(summary = "Get system health", description = "Get comprehensive system health metrics (Admin only)")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        log.info("Admin {} fetching system health", SecurityUtils.getCurrentUserId());
        Map<String, Object> health = systemHealthService.getSystemHealth();
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get system metrics
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get system metrics", description = "Get application performance metrics (Admin only)")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        log.info("Admin {} fetching system metrics", SecurityUtils.getCurrentUserId());
        Map<String, Object> metrics = systemHealthService.getMetrics();
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Health check endpoint (simple)
     */
    @GetMapping("/status")
    @Operation(summary = "Quick health check", description = "Quick health status check (Admin only)")
    public ResponseEntity<Health> getHealthStatus() {
        log.info("Admin {} checking health status", SecurityUtils.getCurrentUserId());
        Health health = systemHealthService.health();
        return ResponseEntity.ok(health);
    }
}
