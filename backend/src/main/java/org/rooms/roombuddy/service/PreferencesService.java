package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.PreferencesRequest;
import org.rooms.roombuddy.dto.response.PreferencesResponse;
import org.rooms.roombuddy.entity.RoommatePreferences;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.RoommatePreferencesRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PreferencesService {
    
    private final RoommatePreferencesRepository preferencesRepository;
    private final UserRepository userRepository;
    
    public PreferencesResponse createPreferences(UUID userId, PreferencesRequest request) {
        log.info("Creating preferences for user: {}", userId);
        
        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Check if preferences already exist
        if (preferencesRepository.existsByUserId(userId)) {
            throw new BadRequestException("Preferences already exist for user: " + userId);
        }
        
        // Validate budget
        if (request.getMinBudget() != null && request.getMaxBudget() != null) {
            if (request.getMinBudget() > request.getMaxBudget()) {
                throw new BadRequestException("Min budget cannot be greater than max budget");
            }
        }
        
        // Validate age range
        if (request.getMinAge() != null && request.getMaxAge() != null) {
            if (request.getMinAge() > request.getMaxAge()) {
                throw new BadRequestException("Min age cannot be greater than max age");
            }
        }
        
        // Create preferences
        RoommatePreferences preferences = RoommatePreferences.builder()
                .user(user)
                .minBudget(request.getMinBudget())
                .maxBudget(request.getMaxBudget())
                .preferredLocations(request.getPreferredLocations())
                .maxDistanceFromCampus(request.getMaxDistanceFromCampus())
                .cleanlinessLevel(request.getCleanlinessLevel())
                .noiseTolerance(request.getNoiseTolerance())
                .socialLevel(request.getSocialLevel())
                .sleepSchedule(parseSleepSchedule(request.getSleepSchedule()))
                .studyTimePreference(parseStudyTimePreference(request.getStudyTimePreference()))
                .smoking(request.getSmoking())
                .drinking(request.getDrinking())
                .pets(request.getPets())
                .guests(request.getGuests())
                .cooking(request.getCooking())
                .dealBreakers(request.getDealBreakers())
                .preferredGender(parsePreferredGender(request.getPreferredGender()))
                .minAge(request.getMinAge())
                .maxAge(request.getMaxAge())
                .sameUniversity(request.getSameUniversity() != null ? request.getSameUniversity() : false)
                .sameFaculty(request.getSameFaculty() != null ? request.getSameFaculty() : false)
                .lookingForRoommate(request.getLookingForRoommate() != null ? request.getLookingForRoommate() : true)
                .build();
        
        RoommatePreferences saved = preferencesRepository.save(preferences);
        log.info("Preferences created successfully for user: {}", userId);
        
        return mapToResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences(UUID userId) {
        log.info("Fetching preferences for user: {}", userId);
        
        RoommatePreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Preferences not found for user: " + userId));
        
        return mapToResponse(preferences);
    }
    
    public PreferencesResponse updatePreferences(UUID userId, PreferencesRequest request) {
        log.info("Updating preferences for user: {}", userId);
        
        RoommatePreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Preferences not found for user: " + userId));
        
        // Validate budget
        if (request.getMinBudget() != null && request.getMaxBudget() != null) {
            if (request.getMinBudget() > request.getMaxBudget()) {
                throw new BadRequestException("Min budget cannot be greater than max budget");
            }
        }
        
        // Validate age range
        if (request.getMinAge() != null && request.getMaxAge() != null) {
            if (request.getMinAge() > request.getMaxAge()) {
                throw new BadRequestException("Min age cannot be greater than max age");
            }
        }
        
        // Update fields
        if (request.getMinBudget() != null) {
            preferences.setMinBudget(request.getMinBudget());
        }
        if (request.getMaxBudget() != null) {
            preferences.setMaxBudget(request.getMaxBudget());
        }
        if (request.getPreferredLocations() != null) {
            preferences.setPreferredLocations(request.getPreferredLocations());
        }
        if (request.getMaxDistanceFromCampus() != null) {
            preferences.setMaxDistanceFromCampus(request.getMaxDistanceFromCampus());
        }
        if (request.getCleanlinessLevel() != null) {
            preferences.setCleanlinessLevel(request.getCleanlinessLevel());
        }
        if (request.getNoiseTolerance() != null) {
            preferences.setNoiseTolerance(request.getNoiseTolerance());
        }
        if (request.getSocialLevel() != null) {
            preferences.setSocialLevel(request.getSocialLevel());
        }
        if (request.getSleepSchedule() != null) {
            preferences.setSleepSchedule(parseSleepSchedule(request.getSleepSchedule()));
        }
        if (request.getStudyTimePreference() != null) {
            preferences.setStudyTimePreference(parseStudyTimePreference(request.getStudyTimePreference()));
        }
        if (request.getSmoking() != null) {
            preferences.setSmoking(request.getSmoking());
        }
        if (request.getDrinking() != null) {
            preferences.setDrinking(request.getDrinking());
        }
        if (request.getPets() != null) {
            preferences.setPets(request.getPets());
        }
        if (request.getGuests() != null) {
            preferences.setGuests(request.getGuests());
        }
        if (request.getCooking() != null) {
            preferences.setCooking(request.getCooking());
        }
        if (request.getDealBreakers() != null) {
            preferences.setDealBreakers(request.getDealBreakers());
        }
        if (request.getPreferredGender() != null) {
            preferences.setPreferredGender(parsePreferredGender(request.getPreferredGender()));
        }
        if (request.getMinAge() != null) {
            preferences.setMinAge(request.getMinAge());
        }
        if (request.getMaxAge() != null) {
            preferences.setMaxAge(request.getMaxAge());
        }
        if (request.getSameUniversity() != null) {
            preferences.setSameUniversity(request.getSameUniversity());
        }
        if (request.getSameFaculty() != null) {
            preferences.setSameFaculty(request.getSameFaculty());
        }
        if (request.getLookingForRoommate() != null) {
            preferences.setLookingForRoommate(request.getLookingForRoommate());
        }
        
        RoommatePreferences updated = preferencesRepository.save(preferences);
        log.info("Preferences updated successfully for user: {}", userId);
        
        return mapToResponse(updated);
    }
    
    public void deletePreferences(UUID userId) {
        log.info("Deleting preferences for user: {}", userId);
        
        if (!preferencesRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("Preferences not found for user: " + userId);
        }
        
        preferencesRepository.deleteByUserId(userId);
        log.info("Preferences deleted successfully for user: {}", userId);
    }
    
    private RoommatePreferences.SleepSchedule parseSleepSchedule(String sleepSchedule) {
        if (sleepSchedule == null) {
            return null;
        }
        try {
            return RoommatePreferences.SleepSchedule.valueOf(sleepSchedule.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid sleep schedule: " + sleepSchedule + ". Must be EARLY_BIRD, NIGHT_OWL, or FLEXIBLE");
        }
    }
    
    private RoommatePreferences.StudyTimePreference parseStudyTimePreference(String studyTimePreference) {
        if (studyTimePreference == null) {
            return null;
        }
        try {
            return RoommatePreferences.StudyTimePreference.valueOf(studyTimePreference.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid study time preference: " + studyTimePreference + ". Must be MORNING, AFTERNOON, EVENING, NIGHT, or FLEXIBLE");
        }
    }
    
    private RoommatePreferences.PreferredGender parsePreferredGender(String preferredGender) {
        if (preferredGender == null) {
            return null;
        }
        try {
            return RoommatePreferences.PreferredGender.valueOf(preferredGender.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid preferred gender: " + preferredGender + ". Must be MALE, FEMALE, or ANY");
        }
    }
    
    private PreferencesResponse mapToResponse(RoommatePreferences preferences) {
        return PreferencesResponse.builder()
                .id(preferences.getId())
                .userId(preferences.getUser().getId())
                .minBudget(preferences.getMinBudget())
                .maxBudget(preferences.getMaxBudget())
                .preferredLocations(preferences.getPreferredLocations())
                .maxDistanceFromCampus(preferences.getMaxDistanceFromCampus())
                .cleanlinessLevel(preferences.getCleanlinessLevel())
                .noiseTolerance(preferences.getNoiseTolerance())
                .socialLevel(preferences.getSocialLevel())
                .sleepSchedule(preferences.getSleepSchedule() != null ? preferences.getSleepSchedule().name() : null)
                .studyTimePreference(preferences.getStudyTimePreference() != null ? preferences.getStudyTimePreference().name() : null)
                .smoking(preferences.getSmoking())
                .drinking(preferences.getDrinking())
                .pets(preferences.getPets())
                .guests(preferences.getGuests())
                .cooking(preferences.getCooking())
                .dealBreakers(preferences.getDealBreakers())
                .preferredGender(preferences.getPreferredGender() != null ? preferences.getPreferredGender().name() : null)
                .minAge(preferences.getMinAge())
                .maxAge(preferences.getMaxAge())
                .sameUniversity(preferences.getSameUniversity())
                .sameFaculty(preferences.getSameFaculty())
                .lookingForRoommate(preferences.getLookingForRoommate())
                .createdAt(preferences.getCreatedAt())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }
}

