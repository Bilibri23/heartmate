package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.request.ApplicationReviewRequest;
import org.rooms.roombay.dto.request.LandlordVerificationApprovalRequest;
import org.rooms.roombay.dto.request.ListingApprovalRequest;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.rooms.roombay.entity.Report;
import org.rooms.roombay.entity.RoomApplication;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AiPrivilegedActionServiceTest {

    @Mock private ApplicationService applicationService;
    @Mock private VisitService visitService;
    @Mock private ListingService listingService;
    @Mock private LandlordVerificationService landlordVerificationService;
    @Mock private PaymentService paymentService;
    @Mock private ReportService reportService;
    @Mock private AnalyticsEventService analyticsEventService;

    @InjectMocks private AiPrivilegedActionService service;

    private static Map<String, Object> target(UUID id) {
        return Map.of("targetId", id.toString(), "reason", "via test");
    }

    @Test
    void landlordAcceptApplicationCallsReviewWithAcceptedStatus() {
        UUID landlordId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();

        AiChatResponse.ToolExecution out = service.execute(
                landlordId, "LANDLORD", AiPrivilegedActionService.ACCEPT_APPLICATION, target(appId));

        assertThat(out.getSuccess()).isTrue();
        ArgumentCaptor<ApplicationReviewRequest> req = ArgumentCaptor.forClass(ApplicationReviewRequest.class);
        verify(applicationService).reviewApplication(eq(appId), eq(landlordId), req.capture());
        assertThat(req.getValue().getStatus()).isEqualTo(RoomApplication.Status.ACCEPTED);
    }

    @Test
    void adminRejectListingPassesRejectedStatus() {
        UUID adminId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        AiChatResponse.ToolExecution out = service.execute(
                adminId, "ADMIN", AiPrivilegedActionService.REJECT_LISTING, target(listingId));

        assertThat(out.getSuccess()).isTrue();
        ArgumentCaptor<ListingApprovalRequest> req = ArgumentCaptor.forClass(ListingApprovalRequest.class);
        verify(listingService).approveOrRejectListing(eq(listingId), eq(adminId), req.capture());
        assertThat(req.getValue().getStatus()).isEqualTo("REJECTED");
        assertThat(req.getValue().getRejectionReason()).isNotBlank();
    }

    @Test
    void adminApproveVerificationUsesParsedType() {
        UUID adminId = UUID.randomUUID();
        UUID verifId = UUID.randomUUID();

        AiChatResponse.ToolExecution out = service.execute(adminId, "ADMIN",
                AiPrivilegedActionService.APPROVE_VERIFICATION,
                Map.of("targetId", verifId.toString(), "verificationType", "BUSINESS"));

        assertThat(out.getSuccess()).isTrue();
        ArgumentCaptor<LandlordVerificationApprovalRequest> req =
                ArgumentCaptor.forClass(LandlordVerificationApprovalRequest.class);
        verify(landlordVerificationService).approveOrRejectVerification(eq(verifId), eq(adminId), req.capture());
        assertThat(req.getValue().getVerificationType()).isEqualTo("BUSINESS");
        assertThat(req.getValue().getStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void adminVerifyPaymentAndResolveReportDispatch() {
        UUID adminId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        assertThat(service.execute(adminId, "ADMIN", AiPrivilegedActionService.VERIFY_PAYMENT, target(paymentId))
                .getSuccess()).isTrue();
        verify(paymentService).verifyPayment(eq(paymentId), eq(adminId), any());

        assertThat(service.execute(adminId, "ADMIN", AiPrivilegedActionService.RESOLVE_REPORT, target(reportId))
                .getSuccess()).isTrue();
        verify(reportService).resolveReport(eq(reportId), eq(adminId), any(), eq(Report.ReportAction.NO_ACTION));
    }

    @Test
    void landlordCannotRunAdminTool() {
        AiChatResponse.ToolExecution out = service.execute(
                UUID.randomUUID(), "LANDLORD", AiPrivilegedActionService.APPROVE_LISTING, target(UUID.randomUUID()));

        assertThat(out.getSuccess()).isFalse();
        assertThat(out.getMessage()).contains("administrators");
        verifyNoInteractions(listingService);
    }

    @Test
    void adminCannotRunLandlordTool() {
        AiChatResponse.ToolExecution out = service.execute(
                UUID.randomUUID(), "ADMIN", AiPrivilegedActionService.ACCEPT_APPLICATION, target(UUID.randomUUID()));

        assertThat(out.getSuccess()).isFalse();
        assertThat(out.getMessage()).contains("landlords");
        verifyNoInteractions(applicationService);
    }

    @Test
    void missingTargetIdFailsCleanly() {
        AiChatResponse.ToolExecution out = service.execute(
                UUID.randomUUID(), "ADMIN", AiPrivilegedActionService.VERIFY_PAYMENT, Map.of());

        assertThat(out.getSuccess()).isFalse();
        assertThat(out.getMessage()).contains("target id");
        verifyNoInteractions(paymentService);
    }

    @Test
    void unknownToolFails() {
        AiChatResponse.ToolExecution out = service.execute(
                UUID.randomUUID(), "ADMIN", "DELETE_EVERYTHING", target(UUID.randomUUID()));

        assertThat(out.getSuccess()).isFalse();
    }
}
