package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rooms.roombay.dto.request.SupportInquiryRequest;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.SupportInquiryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support", description = "Help and support contact")
public class SupportController {

    private final SupportInquiryService supportInquiryService;

    @PostMapping("/inquiries")
    @Operation(summary = "Submit support inquiry", description = "Authenticated users send a message to the team")
    public ResponseEntity<Map<String, Object>> submitInquiry(@Valid @RequestBody SupportInquiryRequest request) {
        supportInquiryService.submit(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Your message was received. We will get back to you by email."));
    }
}
