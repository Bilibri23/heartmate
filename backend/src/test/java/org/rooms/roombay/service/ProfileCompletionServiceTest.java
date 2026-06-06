package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.ProfileRepository;
import org.rooms.roombay.repository.RoommatePreferencesRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.rooms.roombay.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileCompletionServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final RoommatePreferencesRepository roommatePreferencesRepository = mock(RoommatePreferencesRepository.class);
    private final StudentVerificationRepository studentVerificationRepository = mock(StudentVerificationRepository.class);
    private final LandlordVerificationRepository landlordVerificationRepository = mock(LandlordVerificationRepository.class);

    private final ProfileCompletionService service = new ProfileCompletionService(
            userRepository,
            profileRepository,
            roommatePreferencesRepository,
            studentVerificationRepository,
            landlordVerificationRepository
    );

    @Test
    void studentCanMessageAfterContactAndProfileBasicsWithoutFullVerification() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .role(User.UserRole.STUDENT)
                .emailVerified(true)
                .phoneVerified(false)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(profileRepository.existsByUserId(userId)).thenReturn(true);
        when(roommatePreferencesRepository.existsByUserId(userId)).thenReturn(false);
        when(studentVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var status = service.getCompletionStatus(userId);

        assertThat(status.getMissingSteps()).contains("PREFERENCES", "IDENTITY_VERIFICATION");
        assertThat(status.getOperationEligibility()).containsEntry(ProfileCompletionService.OP_MESSAGE, true);
        assertThat(status.getOperationEligibility()).containsEntry(ProfileCompletionService.OP_APPLY, false);
        assertThat(status.getOperationEligibility()).containsEntry(ProfileCompletionService.OP_PAYMENT, false);
    }
}
