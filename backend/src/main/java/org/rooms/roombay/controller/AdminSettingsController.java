package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.PlatformSettingsUpdateRequest;
import org.rooms.roombay.dto.response.ApiResponse;
import org.rooms.roombay.dto.response.PlatformSettingsResponse;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.PlatformSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Settings", description = "Admin APIs for platform settings")
@SecurityRequirement(name = "bearerAuth")
public class AdminSettingsController {

    private final PlatformSettingsService platformSettingsService;

    @GetMapping
    @Operation(summary = "Get platform settings", description = "Get current platform settings (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlatformSettingsResponse>> getSettings() {
        log.info("Admin {} fetching platform settings", SecurityUtils.getCurrentUserId());
        PlatformSettingsResponse settings = platformSettingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings retrieved successfully"));
    }

    @PutMapping
    @Operation(summary = "Update platform settings", description = "Update platform settings (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlatformSettingsResponse>> updateSettings(
            @Valid @RequestBody PlatformSettingsUpdateRequest request) {
        log.info("Admin {} updating platform settings", SecurityUtils.getCurrentUserId());
        PlatformSettingsResponse settings = platformSettingsService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings updated successfully"));
    }
}
