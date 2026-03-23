package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import org.rooms.roombuddy.dto.response.ProfileCompletionResponse;
import org.rooms.roombuddy.entity.LandlordVerification;
import org.rooms.roombuddy.entity.StudentVerification;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.LandlordVerificationRepository;
import org.rooms.roombuddy.repository.ProfileRepository;
import org.rooms.roombuddy.repository.RoommatePreferencesRepository;
import org.rooms.roombuddy.repository.StudentVerificationRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileCompletionService {
    public static final String OP_APPLY = "APPLY";
    public static final String OP_MESSAGE = "MESSAGE";
    public static final String OP_PAYMENT = "PAYMENT";
    public static final String OP_LISTING_PUBLISH = "LISTING_PUBLISH";

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RoommatePreferencesRepository roommatePreferencesRepository;
    private final StudentVerificationRepository studentVerificationRepository;
    private final LandlordVerificationRepository landlordVerificationRepository;

    public ProfileCompletionResponse getCompletionStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<String> completed = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        addStep(Boolean.TRUE.equals(user.getEmailVerified()), "EMAIL_VERIFIED", completed, missing);
        addStep(Boolean.TRUE.equals(user.getPhoneVerified()), "PHONE_VERIFIED", completed, missing);
        addStep(profileRepository.existsByUserId(userId), "PROFILE_BASICS", completed, missing);

        if (user.getRole() == User.UserRole.STUDENT) {
            boolean hasPreferences = roommatePreferencesRepository.existsByUserId(userId);
            boolean isIdentityVerified = studentVerificationRepository.findByUserId(userId)
                    .map(v -> v.getStatus() == StudentVerification.Status.VERIFIED)
                    .orElse(false);
            addStep(hasPreferences, "PREFERENCES", completed, missing);
            addStep(isIdentityVerified, "IDENTITY_VERIFICATION", completed, missing);
        } else if (user.getRole() == User.UserRole.LANDLORD) {
            var landlord = landlordVerificationRepository.findByUserId(userId);
            boolean identityVerified = landlord.map(v -> v.getIdentityStatus() == LandlordVerification.VerificationStatus.VERIFIED).orElse(false);
            boolean businessVerified = landlord.map(v -> v.getBusinessStatus() == LandlordVerification.VerificationStatus.VERIFIED).orElse(false);
            boolean propertyDocs = landlord.map(v ->
                    (v.getPropertyOwnershipDocUrl() != null && !v.getPropertyOwnershipDocUrl().isBlank()) ||
                    (v.getUtilityBillUrl() != null && !v.getUtilityBillUrl().isBlank())
            ).orElse(false);
            boolean payoutDetails = hasPayoutDetails(user);

            addStep(identityVerified, "IDENTITY_VERIFICATION", completed, missing);
            addStep(businessVerified, "BUSINESS_VERIFICATION", completed, missing);
            addStep(propertyDocs, "PROPERTY_DOCS", completed, missing);
            addStep(payoutDetails, "PAYOUT_DETAILS", completed, missing);
        }

        int total = completed.size() + missing.size();
        int percentage = total == 0 ? 0 : Math.round((completed.size() * 100f) / total);

        Map<String, Boolean> operations = new LinkedHashMap<>();
        operations.put(OP_APPLY, canPerform(user, OP_APPLY, missing));
        operations.put(OP_MESSAGE, canPerform(user, OP_MESSAGE, missing));
        operations.put(OP_PAYMENT, canPerform(user, OP_PAYMENT, missing));
        operations.put(OP_LISTING_PUBLISH, canPerform(user, OP_LISTING_PUBLISH, missing));

        return ProfileCompletionResponse.builder()
                .role(user.getRole().name())
                .completionPercentage(percentage)
                .completedSteps(completed)
                .missingSteps(missing)
                .operationEligibility(operations)
                .build();
    }

    public boolean isEligible(UUID userId, String operation) {
        ProfileCompletionResponse status = getCompletionStatus(userId);
        return status.getOperationEligibility().getOrDefault(operation, false);
    }

    private boolean canPerform(User user, String operation, List<String> missing) {
        if (user.getRole() == User.UserRole.ADMIN) {
            return true;
        }
        return switch (operation) {
            case OP_APPLY, OP_MESSAGE, OP_PAYMENT -> user.getRole() == User.UserRole.STUDENT &&
                    !missing.contains("EMAIL_VERIFIED") &&
                    !missing.contains("PHONE_VERIFIED") &&
                    !missing.contains("PROFILE_BASICS") &&
                    !missing.contains("PREFERENCES") &&
                    !missing.contains("IDENTITY_VERIFICATION");
            case OP_LISTING_PUBLISH -> user.getRole() == User.UserRole.LANDLORD &&
                    !missing.contains("EMAIL_VERIFIED") &&
                    !missing.contains("PHONE_VERIFIED") &&
                    !missing.contains("PROFILE_BASICS") &&
                    !missing.contains("IDENTITY_VERIFICATION") &&
                    !missing.contains("BUSINESS_VERIFICATION") &&
                    !missing.contains("PROPERTY_DOCS") &&
                    !missing.contains("PAYOUT_DETAILS");
            default -> false;
        };
    }

    private boolean hasPayoutDetails(User user) {
        boolean hasName = user.getMomoName() != null && !user.getMomoName().isBlank();
        boolean hasNumber = (user.getMtnMomoNumber() != null && !user.getMtnMomoNumber().isBlank()) ||
                (user.getOrangeMoneyNumber() != null && !user.getOrangeMoneyNumber().isBlank());
        return hasName && hasNumber;
    }

    private void addStep(boolean done, String step, List<String> completed, List<String> missing) {
        if (done) {
            completed.add(step);
        } else {
            missing.add(step);
        }
    }
}
