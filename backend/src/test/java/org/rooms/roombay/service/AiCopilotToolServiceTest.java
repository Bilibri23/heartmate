package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.StudentVerification;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.LeaseRepository;
import org.rooms.roombay.repository.ListingFavoriteRepository;
import org.rooms.roombay.repository.PaymentRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.ReportRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiCopilotToolServiceTest {

    private final StudentVerificationRepository studentVerificationRepository = mock(StudentVerificationRepository.class);
    private final RoomApplicationRepository roomApplicationRepository = mock(RoomApplicationRepository.class);
    private final LeaseRepository leaseRepository = mock(LeaseRepository.class);
    private final ListingFavoriteRepository listingFavoriteRepository = mock(ListingFavoriteRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PropertyListingRepository propertyListingRepository = mock(PropertyListingRepository.class);
    private final LandlordVerificationRepository landlordVerificationRepository = mock(LandlordVerificationRepository.class);
    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private final AiCopilotToolService service = new AiCopilotToolService(
            studentVerificationRepository,
            roomApplicationRepository,
            leaseRepository,
            listingFavoriteRepository,
            paymentRepository,
            propertyListingRepository,
            landlordVerificationRepository,
            reportRepository,
            jdbcTemplate
    );

    @Test
    void rolePolicyOnlyAllowsMatchingReadOnlyTools() {
        assertThat(service.isToolAllowed("STUDENT", "tenant_status_snapshot")).isTrue();
        assertThat(service.isToolAllowed("TENANT", "tenant_status_snapshot")).isTrue();
        assertThat(service.isToolAllowed("LANDLORD", "landlord_portfolio_status")).isTrue();
        assertThat(service.isToolAllowed("ADMIN", "admin_ops_snapshot")).isTrue();

        assertThat(service.isToolAllowed("STUDENT", "admin_ops_snapshot")).isFalse();
        assertThat(service.isToolAllowed("LANDLORD", "tenant_status_snapshot")).isFalse();
        assertThat(service.isToolAllowed("ADMIN", "landlord_portfolio_status")).isFalse();
    }

    @Test
    void tenantContextContainsOnlyAggregatedStatusFields() {
        UUID userId = UUID.randomUUID();
        StudentVerification verification = StudentVerification.builder()
                .status(StudentVerification.Status.VERIFIED)
                .build();
        when(studentVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(verification));
        when(roomApplicationRepository.countByStudentId(userId)).thenReturn(4L);
        when(roomApplicationRepository.countByStudentIdAndStatus(userId, RoomApplication.Status.PENDING)).thenReturn(1L);
        when(roomApplicationRepository.countByStudentIdAndStatus(userId, RoomApplication.Status.ACCEPTED)).thenReturn(2L);
        when(leaseRepository.countByStudentId(userId)).thenReturn(1L);
        when(paymentRepository.countByPayerId(userId)).thenReturn(3L);
        when(listingFavoriteRepository.findByUserId(userId)).thenReturn(List.of());

        String context = service.buildToolContext(userId, "STUDENT");

        assertThat(context).contains("Read_only_platform_tools:");
        assertThat(context).contains("tenantStatusSnapshot={verificationStatus=VERIFIED");
        assertThat(context).contains("applicationsAccepted=2");
        assertThat(context).doesNotContain("adminOpsSnapshot", "documentUrl", "password", "token");
        verify(jdbcTemplate).update(anyString(), eq(userId), eq("STUDENT"), eq("tenant_status_snapshot"), eq(true), any());
    }
}
