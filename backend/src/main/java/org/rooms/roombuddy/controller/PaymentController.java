package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.PaymentSubmitRequest;
import org.rooms.roombuddy.dto.response.PaymentResponse;
import org.rooms.roombuddy.security.RequiresCompletion;
import org.rooms.roombuddy.security.SecurityUtils;
import org.rooms.roombuddy.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment management APIs - Mobile Money focused")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/initiate/{leaseId}")
    @RequiresCompletion(operation = "PAYMENT")
    @Operation(summary = "Initiate payment", description = "Get payment instructions for a lease")
    public ResponseEntity<PaymentResponse> initiatePayment(@PathVariable UUID leaseId) {
        UUID studentId = SecurityUtils.getCurrentUserId();
        log.info("Student {} initiating payment for lease {}", studentId, leaseId);
        PaymentResponse payment = paymentService.initiatePayment(studentId, leaseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }
    
    @PostMapping("/submit")
    @RequiresCompletion(operation = "PAYMENT")
    @Operation(summary = "Submit payment proof", description = "Submit Mobile Money payment proof for verification")
    public ResponseEntity<PaymentResponse> submitPaymentProof(@Valid @RequestBody PaymentSubmitRequest request) {
        UUID studentId = SecurityUtils.getCurrentUserId();
        log.info("Student {} submitting payment proof for lease {}", studentId, request.getLeaseId());
        PaymentResponse payment = paymentService.submitPaymentProof(studentId, request);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID paymentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PaymentResponse payment = paymentService.getPaymentById(paymentId, userId);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/my")
    @Operation(summary = "Get my payments", description = "Get all payments where user is payer or recipient")
    public ResponseEntity<Page<PaymentResponse>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<PaymentResponse> payments = paymentService.getMyPayments(userId, pageable);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/lease/{leaseId}")
    @Operation(summary = "Get payments for a lease")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsForLease(
            @PathVariable UUID leaseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<PaymentResponse> payments = paymentService.getPaymentsForLease(leaseId, userId, pageable);
        return ResponseEntity.ok(payments);
    }
    
    @DeleteMapping("/{paymentId}")
    @Operation(summary = "Cancel pending payment", description = "Cancel a pending payment that hasn't been submitted yet")
    public ResponseEntity<Void> cancelPayment(@PathVariable UUID paymentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} cancelling payment {}", userId, paymentId);
        paymentService.cancelPayment(paymentId, userId);
        return ResponseEntity.noContent().build();
    }
}
