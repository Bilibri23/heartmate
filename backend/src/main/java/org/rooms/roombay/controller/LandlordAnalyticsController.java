package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.LandlordAnalyticsResponse;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.LandlordAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/landlord/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Landlord Analytics", description = "Performance metrics for landlords")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('LANDLORD')")
public class LandlordAnalyticsController {

    private final LandlordAnalyticsService landlordAnalyticsService;

    @GetMapping
    @Operation(summary = "Get landlord analytics", description = "Period-aware performance metrics for the current landlord")
    public ResponseEntity<LandlordAnalyticsResponse> getAnalytics(
            @RequestParam(defaultValue = "30d") String range) {
        UUID landlordId = SecurityUtils.getCurrentUserId();
        log.debug("Fetching landlord analytics for {} range={}", landlordId, range);
        return ResponseEntity.ok(landlordAnalyticsService.getAnalytics(landlordId, range));
    }
}
