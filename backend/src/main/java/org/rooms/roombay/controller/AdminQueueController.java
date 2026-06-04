package org.rooms.roombay.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rooms.roombay.dto.request.AdminQueueActionRequest;
import org.rooms.roombay.dto.response.AdminQueueItemResponse;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.AdminQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/queues")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminQueueController {

    private final AdminQueueService adminQueueService;

    @GetMapping
    public ResponseEntity<List<AdminQueueItemResponse>> list(
            @RequestParam(required = false) String queueType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(adminQueueService.list(queueType, priority, status, limit));
    }

    @PostMapping("/{queueType}/{targetId}/actions")
    public ResponseEntity<Map<String, Object>> action(
            @PathVariable String queueType,
            @PathVariable UUID targetId,
            @Valid @RequestBody AdminQueueActionRequest request
    ) {
        return ResponseEntity.ok(adminQueueService.applyAction(
                queueType,
                targetId,
                SecurityUtils.getCurrentUserId(),
                request
        ));
    }
}
