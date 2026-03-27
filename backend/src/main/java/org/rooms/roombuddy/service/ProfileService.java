package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.ProfileRequest;
import org.rooms.roombay.dto.response.ProfileResponse;
import org.rooms.roombay.entity.Profile;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.ProfileRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileResponse createProfile(UUID userId, ProfileRequest request) {
        log.info("Creating profile for user: {}", userId);

        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if profile already exists
        if (profileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Profile already exists for user: " + userId);
        }

        // Prepare languages safely
        List<String> languages = request.getLanguages() != null
                ? Arrays.asList(request.getLanguages())
                : Collections.emptyList();

        // Create profile
        Profile profile = Profile.builder()
                .user(user)
                .bio(request.getBio())
                .profilePhotoUrl(request.getProfilePhotoUrl())
                .languages(languages)
                .whatsappNumber(request.getWhatsappNumber())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .emergencyContactRelationship(request.getEmergencyContactRelationship())
                .visibility(parseVisibility(request.getVisibility()))
                .build();

        Profile savedProfile = profileRepository.save(profile);

        // Update user's profile_completed flag
        user.setProfileCompleted(true);
        userRepository.save(user);

        log.info("Profile created successfully for user: {}", userId);
        return mapToResponse(savedProfile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        log.info("Fetching profile for user: {}", userId);

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(UUID profileId) {
        log.info("Fetching profile by id: {}", profileId);

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + profileId));

        return mapToResponse(profile);
    }

    public ProfileResponse updateProfile(UUID userId, ProfileRequest request) {
        log.info("Updating profile for user: {}", userId);
        log.info("Request bio: '{}', photoUrl: '{}'", request.getBio(), request.getProfilePhotoUrl());

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        // Update fields - handle both null and empty strings properly
        // Bio: allow empty string to save, null means don't update
        if (request.getBio() != null) {
            // Save empty string as-is (don't convert to null) - empty string is a valid value
            profile.setBio(request.getBio());
            log.info("Setting bio to: '{}'", request.getBio());
        }
        // Profile photo URL: allow empty string to clear, null means don't update
        if (request.getProfilePhotoUrl() != null) {
            // Save the URL as-is, even if empty string (don't convert to null)
            profile.setProfilePhotoUrl(request.getProfilePhotoUrl());
            log.info("Setting profilePhotoUrl to: '{}'", request.getProfilePhotoUrl());
        }
        // Languages: always update if provided (even if empty array)
        if (request.getLanguages() != null) {
            profile.setLanguages(request.getLanguages().length > 0 ? Arrays.asList(request.getLanguages()) : Collections.emptyList());
        }
        // WhatsApp number: allow empty string to clear, null means don't update
        if (request.getWhatsappNumber() != null) {
            profile.setWhatsappNumber(request.getWhatsappNumber().isEmpty() ? null : request.getWhatsappNumber());
        }
        // Emergency contact fields: allow empty string to clear, null means don't update
        if (request.getEmergencyContactName() != null) {
            profile.setEmergencyContactName(request.getEmergencyContactName().isEmpty() ? null : request.getEmergencyContactName());
        }
        if (request.getEmergencyContactPhone() != null) {
            profile.setEmergencyContactPhone(request.getEmergencyContactPhone().isEmpty() ? null : request.getEmergencyContactPhone());
        }
        if (request.getEmergencyContactRelationship() != null) {
            profile.setEmergencyContactRelationship(request.getEmergencyContactRelationship().isEmpty() ? null : request.getEmergencyContactRelationship());
        }
        // Visibility: always update if provided
        if (request.getVisibility() != null) {
            profile.setVisibility(parseVisibility(request.getVisibility()));
        }

        Profile updatedProfile = profileRepository.save(profile);
        log.info("Profile updated successfully for user: {}", userId);
        log.info("Saved bio: '{}', photoUrl: '{}'", updatedProfile.getBio(), updatedProfile.getProfilePhotoUrl());
        ProfileResponse response = mapToResponse(updatedProfile);
        log.info("Response bio: '{}', photoUrl: '{}'", response.getBio(), response.getProfilePhotoUrl());
        return response;
    }

    public void deleteProfile(UUID userId) {
        log.info("Deleting profile for user: {}", userId);

        if (!profileRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("Profile not found for user: " + userId);
        }

        profileRepository.deleteByUserId(userId);

        // Update user's profile_completed flag
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setProfileCompleted(false);
        userRepository.save(user);

        log.info("Profile deleted successfully for user: {}", userId);
    }

    private ProfileResponse mapToResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .bio(profile.getBio())
                .profilePhotoUrl(profile.getProfilePhotoUrl())
                .languages(profile.getLanguages())
                .whatsappNumber(profile.getWhatsappNumber())
                .emergencyContactName(profile.getEmergencyContactName())
                .emergencyContactPhone(profile.getEmergencyContactPhone())
                .emergencyContactRelationship(profile.getEmergencyContactRelationship())
                .visibility(profile.getVisibility() != null ? profile.getVisibility().name() : null)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private Profile.Visibility parseVisibility(String visibility) {
        if (visibility == null) {
            return Profile.Visibility.PUBLIC;
        }
        try {
            return Profile.Visibility.valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid visibility value: " + visibility + ". Must be PUBLIC, VERIFIED_ONLY, or PRIVATE");
        }
    }
}

