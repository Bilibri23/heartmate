package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.request.ListingApprovalRequest;
import org.rooms.roombay.dto.request.ListingRequest;
import org.rooms.roombay.dto.response.ListingResponse;
import org.rooms.roombay.entity.PlatformSettings;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.RealtorProfile;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.repository.ListingFavoriteRepository;
import org.rooms.roombay.repository.ListingPhotoRepository;
import org.rooms.roombay.repository.ListingSearchOutboxRepository;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.RealtorProfileRepository;
import org.rooms.roombay.repository.ReviewRepository;
import org.rooms.roombay.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock private PropertyListingRepository listingRepository;
    @Mock private ListingPhotoRepository photoRepository;
    @Mock private ListingFavoriteRepository favoriteRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ListingSearchOutboxRepository listingSearchOutboxRepository;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private LandlordVerificationRepository landlordVerificationRepository;
    @Mock private PlatformSettingsService platformSettingsService;
    @Mock private RealtorProfileRepository realtorProfileRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private AnalyticsEventService analyticsEventService;
    @Mock private NotificationService notificationService;

    @InjectMocks private ListingService service;

    private static User user(UUID id, User.UserRole role) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Grace");
        u.setLastName("Agent");
        u.setRole(role);
        return u;
    }

    private static ListingRequest basicRequest() {
        return ListingRequest.builder()
                .title("Cozy studio")
                .rentAmount(50000)
                .city("Douala")
                .neighborhood("Bonapriso")
                .ownershipDocumentUrl("https://cdn.example.com/doc.pdf")
                .build();
    }

    private void stubCommonCreateCollaborators(UUID userId) {
        when(platformSettingsService.getRawSettings()).thenReturn(
                PlatformSettings.builder().maxListingsPerLandlord(50).autoApproveListings(false).build());
        when(listingRepository.countByLandlordId(userId)).thenReturn(0L);
        when(listingRepository.findRecentEquivalentDraftOrPending(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(listingRepository.save(any())).thenAnswer(inv -> {
            PropertyListing p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(photoRepository.findByListingIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(reviewRepository.getAverageRatingForListing(any())).thenReturn(null);
        when(reviewRepository.countReviewsForListing(any())).thenReturn(0L);
    }

    @Test
    void createListingRejectsUnverifiedRealtor() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, User.UserRole.REALTOR)));
        when(realtorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(
                RealtorProfile.builder().verificationStatus(RealtorProfile.VerificationStatus.PENDING).build()));

        assertThatThrownBy(() -> service.createListing(userId, basicRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("verified realtors");
        verify(listingRepository, never()).save(any());
    }

    @Test
    void createListingRejectsRealtorWithNoProfile() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, User.UserRole.REALTOR)));
        when(realtorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createListing(userId, basicRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Complete your realtor profile");
    }

    @Test
    void createListingByVerifiedRealtorSetsAgencyAndIncrementsCounter() {
        UUID userId = UUID.randomUUID();
        RealtorProfile profile = RealtorProfile.builder()
                .agencyName("Grace Homes")
                .verificationStatus(RealtorProfile.VerificationStatus.VERIFIED)
                .totalListings(2)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, User.UserRole.REALTOR)));
        when(realtorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        stubCommonCreateCollaborators(userId);

        ListingResponse out = service.createListing(userId, basicRequest());

        assertThat(out.getListedByRole()).isEqualTo("REALTOR");
        assertThat(out.getAgencyName()).isEqualTo("Grace Homes");
        assertThat(profile.getTotalListings()).isEqualTo(3);
        verify(realtorProfileRepository).save(profile);
    }

    @Test
    void createListingByLandlordLeavesAgencyNull() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, User.UserRole.LANDLORD)));
        stubCommonCreateCollaborators(userId);

        ListingResponse out = service.createListing(userId, basicRequest());

        assertThat(out.getListedByRole()).isEqualTo("LANDLORD");
        assertThat(out.getAgencyName()).isNull();
        verify(realtorProfileRepository, never()).save(any());
    }

    @Test
    void approveRejectsListingMissingOwnershipDocument() {
        UUID listingId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        PropertyListing listing = PropertyListing.builder()
                .id(listingId)
                .landlord(user(UUID.randomUUID(), User.UserRole.LANDLORD))
                .status(PropertyListing.Status.PENDING)
                .ownershipDocumentUrl(null)
                .build();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(user(adminId, User.UserRole.ADMIN)));

        ListingApprovalRequest req = ListingApprovalRequest.builder().status("ACTIVE").build();

        assertThatThrownBy(() -> service.approveOrRejectListing(listingId, adminId, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("proof-of-ownership");
        verify(listingRepository, never()).save(any());
    }

    @Test
    void approveSucceedsWithOwnershipDocument() {
        UUID listingId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        PropertyListing listing = PropertyListing.builder()
                .id(listingId)
                .landlord(user(UUID.randomUUID(), User.UserRole.LANDLORD))
                .status(PropertyListing.Status.PENDING)
                .ownershipDocumentUrl("https://cdn.example.com/doc.pdf")
                .build();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(user(adminId, User.UserRole.ADMIN)));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(photoRepository.findByListingIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(reviewRepository.getAverageRatingForListing(any())).thenReturn(null);
        when(reviewRepository.countReviewsForListing(any())).thenReturn(0L);

        ListingApprovalRequest req = ListingApprovalRequest.builder().status("ACTIVE").build();

        ListingResponse out = service.approveOrRejectListing(listingId, adminId, req);

        assertThat(out.getVerified()).isTrue();
        assertThat(out.getStatus()).isEqualTo("ACTIVE");
    }
}
