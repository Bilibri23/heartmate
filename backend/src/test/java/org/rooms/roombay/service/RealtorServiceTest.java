package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.request.RealtorProfileRequest;
import org.rooms.roombay.dto.response.RealtorProfileResponse;
import org.rooms.roombay.entity.Notification;
import org.rooms.roombay.entity.RealtorProfile;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.repository.RealtorProfileRepository;
import org.rooms.roombay.repository.RealtorVerificationDocumentRepository;
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
class RealtorServiceTest {

    @Mock private RealtorProfileRepository realtorProfileRepository;
    @Mock private RealtorVerificationDocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private RealtorService service;

    private static User user(UUID id) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Grace");
        u.setLastName("Agent");
        u.setEmail("grace@agency.cm");
        return u;
    }

    @Test
    void registerCreatesProfileWhenNoneExists() {
        UUID userId = UUID.randomUUID();
        when(realtorProfileRepository.existsByUserId(userId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(realtorProfileRepository.save(any())).thenAnswer(inv -> {
            RealtorProfile p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(documentRepository.findByRealtorIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        RealtorProfileRequest req = RealtorProfileRequest.builder()
                .agencyName("Grace Homes")
                .city("Yaoundé")
                .areasCovered(List.of("Melen", "Damas"))
                .build();

        RealtorProfileResponse out = service.registerProfile(userId, req);

        assertThat(out.getAgencyName()).isEqualTo("Grace Homes");
        assertThat(out.getVerificationStatus()).isEqualTo("PENDING");
        assertThat(out.getAreasCovered()).containsExactly("Melen", "Damas");
    }

    @Test
    void registerRejectsDuplicateProfile() {
        UUID userId = UUID.randomUUID();
        when(realtorProfileRepository.existsByUserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> service.registerProfile(userId,
                RealtorProfileRequest.builder().agencyName("X").city("Douala").build()))
                .isInstanceOf(BadRequestException.class);
        verify(realtorProfileRepository, never()).save(any());
    }

    @Test
    void approveMarksVerifiedAndNotifies() {
        UUID realtorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        RealtorProfile profile = RealtorProfile.builder()
                .id(realtorId)
                .user(user(UUID.randomUUID()))
                .agencyName("Grace Homes")
                .verificationStatus(RealtorProfile.VerificationStatus.PENDING)
                .build();
        when(realtorProfileRepository.findById(realtorId)).thenReturn(Optional.of(profile));
        when(realtorProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.findByRealtorIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        RealtorProfileResponse out = service.approve(realtorId, adminId);

        assertThat(out.getVerificationStatus()).isEqualTo("VERIFIED");
        assertThat(profile.getReviewedBy()).isEqualTo(adminId);
        verify(notificationService).createNotification(any(), eq(Notification.NotificationType.VERIFICATION_APPROVED),
                any(), any(), any(), eq("REALTOR"), any());
    }

    @Test
    void rejectStoresReason() {
        UUID realtorId = UUID.randomUUID();
        RealtorProfile profile = RealtorProfile.builder()
                .id(realtorId)
                .user(user(UUID.randomUUID()))
                .verificationStatus(RealtorProfile.VerificationStatus.PENDING)
                .build();
        when(realtorProfileRepository.findById(realtorId)).thenReturn(Optional.of(profile));
        when(realtorProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.findByRealtorIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        RealtorProfileResponse out = service.reject(realtorId, UUID.randomUUID(), "Business registration unreadable");

        assertThat(out.getVerificationStatus()).isEqualTo("REJECTED");
        assertThat(out.getRejectionReason()).isEqualTo("Business registration unreadable");
    }
}
