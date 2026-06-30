package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.PaymentRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.ReportRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.VisitRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQueueActionServiceTest {

    @Mock private RoomApplicationRepository roomApplicationRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private PropertyListingRepository propertyListingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private LandlordVerificationRepository landlordVerificationRepository;

    @InjectMocks private AiQueueActionService service;

    @Test
    void landlordPendingApplicationsBuildsItemsWithConfirmButtons() {
        UUID landlordId = UUID.randomUUID();
        User student = new User();
        student.setFirstName("John");
        student.setLastName("Doe");
        PropertyListing listing = new PropertyListing();
        listing.setTitle("Cozy Studio");
        RoomApplication app = RoomApplication.builder()
                .id(UUID.randomUUID())
                .student(student)
                .listing(listing)
                .moveInDate(LocalDate.now().plusMonths(1))
                .status(RoomApplication.Status.PENDING)
                .build();
        when(roomApplicationRepository.findByLandlordIdAndStatus(
                eq(landlordId), eq(RoomApplication.Status.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));

        Optional<AiChatResponse> out = service.tryQueue(landlordId, "LANDLORD", "show my pending applications", "t1");

        assertThat(out).isPresent();
        assertThat(out.get().getActionItems()).hasSize(1);
        AiChatResponse.ActionItem item = out.get().getActionItems().get(0);
        assertThat(item.getTitle()).contains("John Doe").contains("Cozy Studio");
        assertThat(item.getActions()).hasSize(2);
        AiChatResponse.SuggestedAction accept = item.getActions().get(0);
        assertThat(accept.getType()).isEqualTo("CONFIRM_ACTION");
        assertThat(accept.getTool()).isEqualTo(AiPrivilegedActionService.ACCEPT_APPLICATION);
        assertThat(accept.getActionParams()).containsEntry("targetId", app.getId().toString());
        assertThat(item.getActions().get(1).getTool()).isEqualTo(AiPrivilegedActionService.REJECT_APPLICATION);
    }

    @Test
    void emptyQueueStillReturnsAFriendlyAnswer() {
        UUID landlordId = UUID.randomUUID();
        when(roomApplicationRepository.findByLandlordIdAndStatus(
                eq(landlordId), eq(RoomApplication.Status.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Optional<AiChatResponse> out = service.tryQueue(landlordId, "LANDLORD", "any pending applications?", "t1");

        assertThat(out).isPresent();
        assertThat(out.get().getActionItems()).isEmpty();
        assertThat(out.get().getAnswer().toLowerCase()).contains("no");
    }

    @Test
    void nonQueueMessageReturnsEmpty() {
        assertThat(service.tryQueue(UUID.randomUUID(), "LANDLORD", "what is the weather today", "t1")).isEmpty();
    }

    @Test
    void adminQueueIntentsAreScopedToAdminRole() {
        // A landlord asking about listings should not hit the admin listing queue.
        assertThat(service.tryQueue(UUID.randomUUID(), "LANDLORD", "show pending listings to review", "t1")).isEmpty();
    }
}
