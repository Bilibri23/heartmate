package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import org.rooms.roombay.dto.response.ApplicationTimelineResponse;
import org.rooms.roombay.dto.response.ApplicationTimelineResponse.Step;
import org.rooms.roombay.entity.Lease;
import org.rooms.roombay.entity.Payment;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.Visit;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.LeaseRepository;
import org.rooms.roombay.repository.PaymentRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds a read-only progress timeline for an application by stitching application,
 * visit, lease, and payment state into the roadmap sequence. Purely derived — no writes.
 */
@Service
@RequiredArgsConstructor
public class ApplicationTimelineService {

    private static final String DONE = "DONE";
    private static final String CURRENT = "CURRENT";
    private static final String PENDING = "PENDING";

    private final RoomApplicationRepository applicationRepository;
    private final VisitRepository visitRepository;
    private final LeaseRepository leaseRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public ApplicationTimelineResponse buildTimeline(UUID applicationId, UUID userId) {
        RoomApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        boolean isTenant = app.getStudent().getId().equals(userId);
        boolean isLandlord = app.getListing().getLandlord().getId().equals(userId);
        if (!isTenant && !isLandlord) {
            throw new BadRequestException("You do not have permission to view this timeline");
        }

        List<Visit> visits = visitRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
        if (visits.isEmpty()) {
            visits = visitRepository.findByListingIdAndTenantIdOrderByCreatedAtAsc(
                    app.getListing().getId(), app.getStudent().getId());
        }
        boolean visitScheduled = visits.stream().anyMatch(v ->
                v.getStatus() == Visit.Status.ACCEPTED || v.getStatus() == Visit.Status.RESCHEDULED
                        || v.getStatus() == Visit.Status.COMPLETED);
        boolean visitRequested = visits.stream().anyMatch(v -> v.getStatus() == Visit.Status.REQUESTED);
        boolean visitCompleted = visits.stream().anyMatch(v -> v.getStatus() == Visit.Status.COMPLETED);
        LocalDateTime visitAt = visits.stream()
                .filter(v -> v.getVisitDatetime() != null).map(Visit::getVisitDatetime)
                .findFirst().orElse(null);

        Lease lease = leaseRepository.findByApplicationId(applicationId).orElse(null);
        boolean leasePending = lease != null && (lease.getStatus() == Lease.LeaseStatus.PENDING_PAYMENT
                || lease.getStatus() == Lease.LeaseStatus.PENDING_SIGNATURES);
        boolean leaseProgressed = lease != null && (lease.getStatus() == Lease.LeaseStatus.ACTIVE
                || lease.getStatus() == Lease.LeaseStatus.COMPLETED
                || lease.getStatus() == Lease.LeaseStatus.TERMINATED);
        boolean leaseActive = lease != null && lease.getStatus() == Lease.LeaseStatus.ACTIVE;

        boolean paymentSubmitted = false;
        boolean paymentVerified = false;
        if (lease != null) {
            List<Payment> payments = paymentRepository.findByLeaseId(lease.getId(),
                    org.springframework.data.domain.Pageable.unpaged()).getContent();
            paymentVerified = payments.stream().anyMatch(p -> p.getStatus() == Payment.PaymentStatus.VERIFIED);
            paymentSubmitted = paymentVerified || payments.stream().anyMatch(p ->
                    p.getStatus() == Payment.PaymentStatus.SUBMITTED);
        }

        boolean reviewed = app.getReviewedAt() != null;
        boolean accepted = app.getStatus() == RoomApplication.Status.ACCEPTED;

        List<Step> steps = new ArrayList<>();
        steps.add(step("submitted", "Application submitted", DONE, app.getCreatedAt()));
        steps.add(step("reviewing", "Landlord reviewing", reviewed ? DONE : CURRENT, app.getReviewedAt()));
        steps.add(step("visit_scheduled", "Visit scheduled",
                visitScheduled ? DONE : (visitRequested ? CURRENT : PENDING), visitAt));
        steps.add(step("visit_completed", "Visit completed", visitCompleted ? DONE : PENDING, null));
        steps.add(step("lease_pending", "Lease pending",
                leaseProgressed ? DONE : (leasePending || accepted ? CURRENT : PENDING),
                lease == null ? null : lease.getCreatedAt()));
        steps.add(step("payment_submitted", "Payment proof submitted", paymentSubmitted ? DONE : PENDING, null));
        steps.add(step("payment_verified", "Payment verified", paymentVerified ? DONE : PENDING, null));
        steps.add(step("move_in", "Move-in confirmed", leaseActive ? DONE : PENDING, null));

        return ApplicationTimelineResponse.builder()
                .applicationId(applicationId)
                .steps(steps)
                .build();
    }

    private static Step step(String key, String label, String status, LocalDateTime at) {
        return Step.builder().key(key).label(label).status(status).at(at).build();
    }
}
