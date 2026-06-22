package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.VisitRequest;
import org.rooms.roombay.dto.request.VisitUpdateRequest;
import org.rooms.roombay.dto.response.VisitResponse;
import org.rooms.roombay.entity.Visit;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.VisitService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Visits", description = "APIs for scheduling and managing property visits")
@SecurityRequirement(name = "bearerAuth")
public class VisitController {

    private final VisitService visitService;

    /** Tenant requests a visit. */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Request a visit", description = "Tenant requests to view a listing")
    public ResponseEntity<VisitResponse> requestVisit(@Valid @RequestBody VisitRequest request) {
        UUID tenantId = SecurityUtils.getCurrentUserId();
        log.info("Tenant {} requesting visit to listing {}", tenantId, request.getListingId());
        VisitResponse response = visitService.requestVisit(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Get a single visit (tenant or landlord party to it). */
    @GetMapping("/{visitId}")
    @Operation(summary = "Get visit by ID")
    public ResponseEntity<VisitResponse> getVisit(@PathVariable UUID visitId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(visitService.getVisit(visitId, userId));
    }

    /** Tenant's own visit requests. */
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get my visits")
    public ResponseEntity<Page<VisitResponse>> getMyVisits(
            @RequestParam(required = false) Visit.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(visitService.getTenantVisits(tenantId, status, pageable));
    }

    /** Landlord's received visit requests across their listings. */
    @GetMapping("/landlord/received")
    @PreAuthorize("hasRole('LANDLORD')")
    @Operation(summary = "Get received visits")
    public ResponseEntity<Page<VisitResponse>> getReceivedVisits(
            @RequestParam(required = false) Visit.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID landlordId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(visitService.getLandlordVisits(landlordId, status, pageable));
    }

    /** Landlord accepts / reschedules / cancels / completes / no-show. */
    @PutMapping("/{visitId}")
    @PreAuthorize("hasRole('LANDLORD')")
    @Operation(summary = "Update visit", description = "Landlord accepts, reschedules, cancels, completes, or marks no-show")
    public ResponseEntity<VisitResponse> updateVisit(
            @PathVariable UUID visitId,
            @Valid @RequestBody VisitUpdateRequest request) {
        UUID landlordId = SecurityUtils.getCurrentUserId();
        log.info("Landlord {} updating visit {} -> {}", landlordId, visitId, request.getStatus());
        return ResponseEntity.ok(visitService.updateVisit(visitId, landlordId, request));
    }

    /** Tenant cancels their own visit. */
    @PutMapping("/{visitId}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Cancel my visit")
    public ResponseEntity<VisitResponse> cancelVisit(
            @PathVariable UUID visitId,
            @RequestBody(required = false) Map<String, String> body) {
        UUID tenantId = SecurityUtils.getCurrentUserId();
        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(visitService.cancelByTenant(visitId, tenantId, reason));
    }

    /** Visit stats for the current user (role-aware). */
    @GetMapping("/stats")
    @Operation(summary = "Get visit statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();
        if ("STUDENT".equals(role)) {
            return ResponseEntity.ok(visitService.getTenantStats(userId));
        } else if ("LANDLORD".equals(role)) {
            return ResponseEntity.ok(visitService.getLandlordStats(userId));
        }
        return ResponseEntity.badRequest().build();
    }
}
