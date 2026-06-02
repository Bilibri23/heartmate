package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.PaymentSubmitRequest;
import org.rooms.roombay.dto.response.PaymentResponse;
import org.rooms.roombay.entity.Lease;
import org.rooms.roombay.entity.Payment;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.LeaseRepository;
import org.rooms.roombay.repository.PaymentRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;
    private final LeaseService leaseService;
    private final NotificationService notificationService;
    private final AnalyticsEventService analyticsEventService;

    @Value("${roombay.payments.platform-mtn-momo:670000000}")
    private String platformMtnMomoNumber;

    @Value("${roombay.payments.platform-orange-money:690000000}")
    private String platformOrangeMoneyNumber;

    // Pass-through MVP: the platform does not take a fee; fees can be reintroduced later
    private static final double PLATFORM_FEE_PERCENTAGE = 0.0; // 0% commission for MVP
    
    public PaymentResponse initiatePayment(UUID studentId, UUID leaseId) {
        log.info("Initiating payment for lease: {} by student: {}", leaseId, studentId);
        
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        if (!lease.getStudent().getId().equals(studentId)) {
            throw new BadRequestException("You can only pay for your own leases");
        }
        
        if (lease.getStatus() != Lease.LeaseStatus.PENDING_PAYMENT) {
            throw new BadRequestException("This lease is not awaiting payment. Status: " + lease.getStatus());
        }
        
        // Check if payment already exists
        if (paymentRepository.existsByLeaseIdAndPaymentTypeAndStatusIn(
                leaseId, Payment.PaymentType.INITIAL_PAYMENT,
                Arrays.asList(Payment.PaymentStatus.PENDING, Payment.PaymentStatus.SUBMITTED))) {
            throw new BadRequestException("A payment is already pending for this lease");
        }
        
        int amount = lease.getTotalInitialPayment();
        int platformFee = (int) (amount * PLATFORM_FEE_PERCENTAGE);
        int landlordPayout = amount - platformFee;
        
        Payment payment = Payment.builder()
                .lease(lease)
                .payer(lease.getStudent())
                .recipient(lease.getLandlord())
                .amount(amount)
                .paymentType(Payment.PaymentType.INITIAL_PAYMENT)
                .paymentMethod(Payment.PaymentMethod.MTN_MOMO)
                .status(Payment.PaymentStatus.PENDING)
                .platformFee(platformFee)
                .landlordPayout(landlordPayout)
                .build();
        
        Payment saved = paymentRepository.save(payment);
        log.info("Payment initiated: {} with reference: {}", saved.getId(), saved.getReferenceCode());
        
        return mapToResponseWithInstructions(saved);
    }
    
    public PaymentResponse submitPaymentProof(UUID studentId, PaymentSubmitRequest request) {
        log.info("Submitting payment proof for lease: {} by student: {}", request.getLeaseId(), studentId);
        
        Lease lease = leaseRepository.findById(request.getLeaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        if (!lease.getStudent().getId().equals(studentId)) {
            throw new BadRequestException("You can only submit payment for your own leases");
        }

        if (lease.getStatus() != Lease.LeaseStatus.PENDING_PAYMENT) {
            throw new BadRequestException("This lease is not awaiting payment. Status: " + lease.getStatus());
        }

        if (paymentRepository.existsByLeaseIdAndPaymentTypeAndStatusIn(
                lease.getId(),
                Payment.PaymentType.INITIAL_PAYMENT,
                Arrays.asList(Payment.PaymentStatus.SUBMITTED, Payment.PaymentStatus.VERIFIED))) {
            throw new BadRequestException("Payment already submitted for this lease");
        }
        
        // Find pending payment or create new one
        Payment payment = paymentRepository.findByLeaseIdAndStatus(request.getLeaseId(), Payment.PaymentStatus.PENDING)
                .stream().findFirst()
                .orElseGet(() -> {
                    int amount = lease.getTotalInitialPayment();
                    int platformFee = (int) (amount * PLATFORM_FEE_PERCENTAGE);
                    return Payment.builder()
                            .lease(lease)
                            .payer(lease.getStudent())
                            .recipient(lease.getLandlord())
                            .amount(amount)
                            .paymentType(Payment.PaymentType.INITIAL_PAYMENT)
                            .platformFee(platformFee)
                            .landlordPayout(amount - platformFee)
                            .build();
                });
        
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setMomoPhoneNumber(request.getMomoPhoneNumber());
        payment.setMomoTransactionId(request.getMomoTransactionId());
        payment.setPaymentProofUrl(request.getPaymentProofUrl());
        payment.setPayerNotes(request.getPayerNotes());
        payment.setStatus(Payment.PaymentStatus.SUBMITTED);
        
        Payment saved = paymentRepository.save(payment);
        log.info("Payment proof submitted: {}", saved.getId());
        analyticsEventService.emit("payment_proof_uploaded", studentId, "STUDENT", lease.getListing().getId(),
                java.util.Map.of("paymentId", saved.getId().toString(), "leaseId", lease.getId().toString()));
        
        return mapToResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId, UUID userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        
        if (!payment.getPayer().getId().equals(userId) && !payment.getRecipient().getId().equals(userId)) {
            throw new BadRequestException("You don't have access to this payment");
        }
        
        return mapToResponse(payment);
    }
    
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsForLease(UUID leaseId, UUID userId, Pageable pageable) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        if (!lease.getStudent().getId().equals(userId) && !lease.getLandlord().getId().equals(userId)) {
            throw new BadRequestException("You don't have access to this lease's payments");
        }
        
        return paymentRepository.findByLeaseId(leaseId, pageable).map(this::mapToResponseWithInstructions);
    }
    
    // Admin methods
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPendingVerifications(Pageable pageable) {
        return paymentRepository.findPendingVerification(pageable).map(this::mapToResponse);
    }
    
    public PaymentResponse verifyPayment(UUID paymentId, UUID adminId, String notes) {
        log.info("Admin {} verifying payment: {}", adminId, paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        
        if (payment.getStatus() != Payment.PaymentStatus.SUBMITTED) {
            throw new BadRequestException("Payment is not awaiting verification. Status: " + payment.getStatus());
        }
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        
        payment.setStatus(Payment.PaymentStatus.VERIFIED);
        payment.setVerifiedBy(admin);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setVerificationNotes(notes);
        
        Payment saved = paymentRepository.save(payment);
        
        // Update lease status
        leaseService.markPaymentReceived(payment.getLease().getId());
        
        // Notify student that payment was verified
        notificationService.notifyPaymentVerified(
                payment.getPayer().getId(),
                payment.getId(),
                String.valueOf(payment.getAmount())
        );
        
        // Notify landlord that payment was received
        notificationService.notifyPaymentReceived(
                payment.getRecipient().getId(),
                payment.getId(),
                String.valueOf(payment.getAmount()),
                payment.getPayer().getFirstName() + " " + payment.getPayer().getLastName()
        );
        
        log.info("Payment {} verified by admin {}", paymentId, adminId);
        
        return mapToResponse(saved);
    }
    
    public PaymentResponse rejectPayment(UUID paymentId, UUID adminId, String reason) {
        log.info("Admin {} rejecting payment: {}", adminId, paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        
        if (payment.getStatus() != Payment.PaymentStatus.SUBMITTED) {
            throw new BadRequestException("Payment is not awaiting verification");
        }
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        
        payment.setStatus(Payment.PaymentStatus.REJECTED);
        payment.setVerifiedBy(admin);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setRejectionReason(reason);
        
        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} rejected by admin {}", paymentId, adminId);
        
        // Notify student that payment was rejected
        notificationService.notifyPaymentRejected(
                payment.getPayer().getId(),
                payment.getId(),
                reason
        );
        
        return mapToResponse(saved);
    }
    
    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .referenceCode(payment.getReferenceCode())
                .leaseId(payment.getLease().getId())
                .leaseReferenceCode(payment.getLease().getReferenceCode())
                .listingTitle(payment.getLease().getListing() != null ? payment.getLease().getListing().getTitle() : null)
                .payerId(payment.getPayer().getId())
                .payerName(payment.getPayer().getFirstName() + " " + payment.getPayer().getLastName())
                .payerEmail(payment.getPayer().getEmail())
                .recipientId(payment.getRecipient().getId())
                .recipientName(payment.getRecipient().getFirstName() + " " + payment.getRecipient().getLastName())
                .amount(payment.getAmount())
                .paymentType(payment.getPaymentType())
                .paymentMethod(payment.getPaymentMethod())
                .momoPhoneNumber(payment.getMomoPhoneNumber())
                .momoTransactionId(payment.getMomoTransactionId())
                .momoReference(payment.getMomoReference())
                .status(payment.getStatus())
                .paymentProofUrl(payment.getPaymentProofUrl())
                .payerNotes(payment.getPayerNotes())
                .verifiedAt(payment.getVerifiedAt())
                .verificationNotes(payment.getVerificationNotes())
                .rejectionReason(payment.getRejectionReason())
                .platformFee(payment.getPlatformFee())
                .landlordPayout(payment.getLandlordPayout())
                .payoutStatus(payment.getPayoutStatus())
                .payoutAt(payment.getPayoutAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
    
    private PaymentResponse mapToResponseWithInstructions(Payment payment) {
        PaymentResponse response = mapToResponse(payment);
        
        // Get landlord's MoMo numbers, fallback to platform defaults
        User landlord = payment.getRecipient();
        String mtnNumber = landlord.getMtnMomoNumber() != null ? landlord.getMtnMomoNumber() : platformMtnMomoNumber;
        String orangeNumber = landlord.getOrangeMoneyNumber() != null ? landlord.getOrangeMoneyNumber() : platformOrangeMoneyNumber;
        String recipientName = landlord.getMomoName() != null ? landlord.getMomoName() : 
                               (landlord.getFirstName() + " " + landlord.getLastName());
        
        response.setPaymentInstructions(PaymentResponse.PaymentInstructions.builder()
                .mtnMomoNumber(mtnNumber)
                .orangeMoneyNumber(orangeNumber)
                .recipientName(recipientName)
                .referenceToUse(payment.getReferenceCode())
                .amountToPay(payment.getAmount())
                .instructions(String.format(
                    "1. Open your Mobile Money app (MTN MOMO or Orange Money)\n" +
                    "2. Send %d XAF to %s:\n" +
                    "   - MTN MOMO: %s\n" +
                    "   - Orange Money: %s\n" +
                    "3. Use reference: %s\n" +
                    "4. Take a screenshot of the confirmation\n" +
                    "5. Upload the screenshot as proof of payment\n" +
                    "6. Wait for admin verification (usually within 24 hours)",
                    payment.getAmount(), recipientName, mtnNumber, orangeNumber, payment.getReferenceCode()
                ))
                .build());
        
        return response;
    }
    
    /**
     * Cancel a pending payment
     */
    @Transactional
    public void cancelPayment(UUID paymentId, UUID userId) {
        log.info("Cancelling payment: {} by user: {}", paymentId, userId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        
        // Verify user owns this payment
        if (!payment.getPayer().getId().equals(userId)) {
            throw new BadRequestException("You can only cancel your own payments");
        }
        
        // Only pending payments can be cancelled
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new BadRequestException("Only pending payments can be cancelled. Current status: " + payment.getStatus());
        }
        
        // Delete the payment
        paymentRepository.delete(payment);
        log.info("Payment {} cancelled successfully", paymentId);
    }
}
