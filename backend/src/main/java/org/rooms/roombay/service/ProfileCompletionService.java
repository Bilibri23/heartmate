package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import org.rooms.roombay.dto.response.ProfileCompletionResponse;
import org.rooms.roombay.entity.LandlordVerification;
import org.rooms.roombay.entity.StudentVerification;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.ProfileRepository;
import org.rooms.roombay.repository.RoommatePreferencesRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.rooms.roombay.repository.UserRepository;
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

        // One contact channel is enough: email OR phone verified satisfies this step.
        boolean contactVerified =
                Boolean.TRUE.equals(user.getEmailVerified()) || Boolean.TRUE.equals(user.getPhoneVerified());
        addStep(contactVerified, "CONTACT_VERIFIED", completed, missing);

        if (user.getRole() == User.UserRole.STUDENT) {
            boolean hasPreferences = roommatePreferencesRepository.existsByUserId(userId);
            boolean isIdentityVerified = studentVerificationRepository.findByUserId(userId)
                    .map(v -> v.getStatus() == StudentVerification.Status.VERIFIED)
                    .orElse(false);
            addStep(hasProfileBasics(userId, user, isIdentityVerified), "PROFILE_BASICS", completed, missing);
            addStep(hasPreferences, "PREFERENCES", completed, missing);
            addStep(isIdentityVerified, "IDENTITY_VERIFICATION", completed, missing);
        } else if (user.getRole() == User.UserRole.LANDLORD) {
            var landlord = landlordVerificationRepository.findByUserId(userId);
            boolean identityVerified = landlord.map(v -> v.getIdentityStatus() == LandlordVerification.VerificationStatus.VERIFIED).orElse(false);
            boolean propertyDocs = landlord.map(v ->
                    (v.getPropertyOwnershipDocUrl() != null && !v.getPropertyOwnershipDocUrl().isBlank()) ||
                    (v.getUtilityBillUrl() != null && !v.getUtilityBillUrl().isBlank())
            ).orElse(false);
            addStep(hasProfileBasics(userId, user, identityVerified), "PROFILE_BASICS", completed, missing);
            addStep(identityVerified, "IDENTITY_VERIFICATION", completed, missing);
            // Business KYC is optional for landlords — not part of completion % or publish gate.
            addStep(propertyDocs, "PROPERTY_DOCS", completed, missing);
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
            case OP_MESSAGE -> user.getRole() == User.UserRole.STUDENT &&
                    !missing.contains("CONTACT_VERIFIED") &&
                    !missing.contains("PROFILE_BASICS");
            case OP_APPLY, OP_PAYMENT -> user.getRole() == User.UserRole.STUDENT &&
                    !missing.contains("CONTACT_VERIFIED") &&
                    !missing.contains("PROFILE_BASICS") &&
                    !missing.contains("PREFERENCES") &&
                    !missing.contains("IDENTITY_VERIFICATION");
            case OP_LISTING_PUBLISH -> user.getRole() == User.UserRole.LANDLORD &&
                    !missing.contains("CONTACT_VERIFIED") &&
                    !missing.contains("PROFILE_BASICS") &&
                    !missing.contains("IDENTITY_VERIFICATION") &&
                    !missing.contains("PROPERTY_DOCS");
            default -> false;
        };
    }

    private void addStep(boolean done, String step, List<String> completed, List<String> missing) {
        if (done) {
            completed.add(step);
        } else {
            missing.add(step);
        }
    }

    private boolean hasProfileBasics(UUID userId, User user, boolean identityVerified) {
        if (profileRepository.existsByUserId(userId)) {
            return true;
        }
        if (Boolean.TRUE.equals(user.getProfileCompleted())) {
            return true;
        }
        // Legacy verified accounts may not have a row in profiles yet.
        return identityVerified;
    }
}
