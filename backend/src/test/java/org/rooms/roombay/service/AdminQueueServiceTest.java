package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.request.AdminQueueActionRequest;
import org.rooms.roombay.dto.response.AdminQueueItemResponse;
import org.rooms.roombay.entity.Payment;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.Report;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.StudentVerification;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.PaymentRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.ReportRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminQueueServiceTest {

    @Mock StudentVerificationRepository studentVerificationRepository;
    @Mock LandlordVerificationRepository landlordVerificationRepository;
    @Mock PropertyListingRepository listingRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ReportRepository reportRepository;
    @Mock RoomApplicationRepository applicationRepository;
    @Mock AdminOpsService adminOpsService;
    @Mock VerificationService verificationService;
    @Mock ListingService listingService;
    @Mock PaymentService paymentService;
    @Mock ReportService reportService;

    @Test
    void listReturnsFilteredQueueItems() {
        AdminQueueService service = service();
        User landlord = User.builder().id(UUID.randomUUID()).email("landlord@example.com").build();
        PropertyListing listing = PropertyListing.builder()
                .id(UUID.randomUUID())
                .landlord(landlord)
                .title("Studio near campus")
                .city("Buea")
                .neighborhood("Molyko")
                .status(PropertyListing.Status.PENDING)
                .createdAt(LocalDateTime.now().minusHours(5))
                .build();

        when(studentVerificationRepository.findByStatusOrderByCreatedAtAsc(eq(StudentVerification.Status.PENDING), any(Pageable.class)))
                .thenReturn(List.of());
        when(landlordVerificationRepository.findAllPendingVerifications(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(listingRepository.findPendingListings(any(Pageable.class))).thenReturn(List.of(listing));
        when(paymentRepository.findPendingVerification(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(reportRepository.findByStatusOrderByCreatedAtDesc(eq(Report.ReportStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(applicationRepository.findStaleActiveApplications(any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of());
        when(adminOpsService.topNoAnswerQuestions(eq("7d"), eq(10))).thenReturn(List.of());
        when(adminOpsService.alerts()).thenReturn(List.of());

        List<AdminQueueItemResponse> items = service.list("LISTING_REVIEW", "HIGH", null, 20);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getQueueType()).isEqualTo("LISTING_REVIEW");
        assertThat(items.get(0).getAvailableActions()).containsExactly("APPROVE", "REJECT");
    }

    @Test
    void rejectActionsRequireNote() {
        AdminQueueService service = service();

        assertThatThrownBy(() -> service.applyAction(
                "LISTING_REVIEW",
                UUID.randomUUID(),
                UUID.randomUUID(),
                AdminQueueActionRequest.builder().action("REJECT").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("note is required");
    }

    private AdminQueueService service() {
        return new AdminQueueService(
                studentVerificationRepository,
                landlordVerificationRepository,
                listingRepository,
                paymentRepository,
                reportRepository,
                applicationRepository,
                adminOpsService,
                verificationService,
                listingService,
                paymentService,
                reportService
        );
    }
}
