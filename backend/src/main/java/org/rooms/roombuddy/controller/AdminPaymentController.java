package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.PaymentResponse;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Payments", description = "Admin APIs for payment verification")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {
    
    private final PaymentService paymentService;
    
    @GetMapping("/pending")
    @Operation(summary = "Get pending verifications", description = "Get payments awaiting verification")
    public ResponseEntity<Page<PaymentResponse>> getPendingVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Admin {} fetching pending payment verifications", SecurityUtils.getCurrentUserId());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<PaymentResponse> payments = paymentService.getPendingVerifications(pageable);
        return ResponseEntity.ok(payments);
    }
    
    @PostMapping("/{paymentId}/verify")
    @Operation(summary = "Verify payment", description = "Verify a submitted payment")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) String notes) {
        
        UUID adminId = SecurityUtils.getCurrentUserId();
        log.info("Admin {} verifying payment {}", adminId, paymentId);
        PaymentResponse payment = paymentService.verifyPayment(paymentId, adminId, notes);
        return ResponseEntity.ok(payment);
    }
    
    @PostMapping("/{paymentId}/reject")
    @Operation(summary = "Reject payment", description = "Reject a submitted payment")
    public ResponseEntity<PaymentResponse> rejectPayment(
            @PathVariable UUID paymentId,
            @RequestParam String reason) {
        
        UUID adminId = SecurityUtils.getCurrentUserId();
        log.info("Admin {} rejecting payment {} for: {}", adminId, paymentId, reason);
        PaymentResponse payment = paymentService.rejectPayment(paymentId, adminId, reason);
        return ResponseEntity.ok(payment);
    }
}
