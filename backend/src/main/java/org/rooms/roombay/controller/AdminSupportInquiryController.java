package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.SupportInquiryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Help and Support messages from {@code /api/support/inquiries} (separate from moderation {@code reports}). */
@RestController
@RequestMapping("/api/admin/support-inquiries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Support Inquiries", description = "Help and Support inbox")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportInquiryController {

    private final SupportInquiryService supportInquiryService;

    @GetMapping
    @Operation(summary = "List support inquiries", description = "Messages submitted from Help and Support in the app")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin {} listing support inquiries page={} size={}", SecurityUtils.getCurrentUserId(), page, size);
        return ResponseEntity.ok(supportInquiryService.listForAdmin(page, size));
    }
}
