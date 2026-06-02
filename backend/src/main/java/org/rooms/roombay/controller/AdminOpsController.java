package org.rooms.roombay.controller;

import lombok.RequiredArgsConstructor;
import org.rooms.roombay.service.AdminOpsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ops")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOpsController {

    private final AdminOpsService adminOpsService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = adminOpsService.health();
        health.put("aiHealth", adminOpsService.aiHealth());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/errors")
    public ResponseEntity<List<Map<String, Object>>> errors(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(adminOpsService.recentErrors(limit));
    }

    @GetMapping("/funnel")
    public ResponseEntity<Map<String, Object>> funnel(@RequestParam(defaultValue = "7d") String range) {
        return ResponseEntity.ok(adminOpsService.funnel(range));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<Map<String, Object>>> alerts() {
        return ResponseEntity.ok(adminOpsService.alerts());
    }
}
