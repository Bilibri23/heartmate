package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.entity.LandlordVerification;
import org.rooms.roombay.entity.StudentVerification;
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

    @Test
    void verifiedStudentLegacyAccountDoesNotNeedToVerifyAgainWhenProfileRowIsMissing() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .role(User.UserRole.STUDENT)
                .emailVerified(true)
                .phoneVerified(false)
                .build();
        StudentVerification verification = StudentVerification.builder()
                .user(user)
                .status(StudentVerification.Status.VERIFIED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        when(roommatePreferencesRepository.existsByUserId(userId)).thenReturn(true);
        when(studentVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(verification));

        var status = service.getCompletionStatus(userId);

        assertThat(status.getCompletedSteps()).contains("PROFILE_BASICS", "IDENTITY_VERIFICATION");
        assertThat(status.getMissingSteps()).doesNotContain("PROFILE_BASICS", "IDENTITY_VERIFICATION");
        assertThat(status.getOperationEligibility()).containsEntry(ProfileCompletionService.OP_MESSAGE, true);
        assertThat(status.getOperationEligibility()).containsEntry(ProfileCompletionService.OP_APPLY, true);
    }

    @Test
    void verifiedLandlordLegacyAccountDoesNotNeedToVerifyAgainWhenProfileRowIsMissing() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .role(User.UserRole.LANDLORD)
                .emailVerified(true)
                .phoneVerified(false)
                .build();
        LandlordVerification verification = LandlordVerification.builder()
                .user(user)
                .identityStatus(LandlordVerification.VerificationStatus.VERIFIED)
                .propertyOwnershipDocUrl("https://res.cloudinary.com/example/image/upload/property-doc.jpg")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        when(landlordVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(verification));

        var status = service.getCompletionStatus(userId);

        assertThat(status.getCompletedSteps()).contains("PROFILE_BASICS", "IDENTITY_VERIFICATION", "PROPERTY_DOCS");
        assertThat(status.getMissingSteps()).doesNotContain("PROFILE_BASICS", "IDENTITY_VERIFICATION");
        assertThat(status.getOperationEligibility()).containsEntry(ProfileCompletionService.OP_LISTING_PUBLISH, true);
    }
}
