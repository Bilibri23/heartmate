package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.ListingPreferencesRequest;
import org.rooms.roombuddy.dto.response.ListingPreferencesResponse;
import org.rooms.roombuddy.entity.ListingPreferences;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.ListingPreferencesRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingPreferencesService {
    
    private final ListingPreferencesRepository listingPreferencesRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public ListingPreferencesResponse createPreferences(UUID userId, ListingPreferencesRequest request) {
        if (listingPreferencesRepository.findByUserId(userId).isPresent()) {
            throw new BadRequestException("Listing preferences already exist for user: " + userId);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        ListingPreferences preferences = ListingPreferences.builder()
                .user(user)
                .minBudget(request.getMinBudget())
                .maxBudget(request.getMaxBudget())
                .preferredLocations(request.getPreferredLocations())
                .maxDistanceFromCampus(request.getMaxDistanceFromCampus())
                .propertyTypes(request.getPropertyTypes())
                .build();
        
        ListingPreferences saved = listingPreferencesRepository.save(preferences);
        log.info("Listing preferences created for user {}", userId);
        return mapToResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public ListingPreferencesResponse getPreferences(UUID userId) {
        ListingPreferences preferences = listingPreferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing preferences not found for user: " + userId));
        return mapToResponse(preferences);
    }
    
    @Transactional
    public ListingPreferencesResponse updatePreferences(UUID userId, ListingPreferencesRequest request) {
        ListingPreferences preferences = listingPreferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing preferences not found for user: " + userId));
        
        preferences.setMinBudget(request.getMinBudget());
        preferences.setMaxBudget(request.getMaxBudget());
        preferences.setPreferredLocations(request.getPreferredLocations());
        preferences.setMaxDistanceFromCampus(request.getMaxDistanceFromCampus());
        preferences.setPropertyTypes(request.getPropertyTypes());
        
        ListingPreferences saved = listingPreferencesRepository.save(preferences);
        log.info("Listing preferences updated for user {}", userId);
        return mapToResponse(saved);
    }
    
    @Transactional
    public void deletePreferences(UUID userId) {
        ListingPreferences preferences = listingPreferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing preferences not found for user: " + userId));
        listingPreferencesRepository.delete(preferences);
        log.info("Listing preferences deleted for user {}", userId);
    }
    
    private ListingPreferencesResponse mapToResponse(ListingPreferences preferences) {
        return ListingPreferencesResponse.builder()
                .id(preferences.getId())
                .userId(preferences.getUser().getId())
                .minBudget(preferences.getMinBudget())
                .maxBudget(preferences.getMaxBudget())
                .preferredLocations(preferences.getPreferredLocations())
                .maxDistanceFromCampus(preferences.getMaxDistanceFromCampus())
                .propertyTypes(preferences.getPropertyTypes())
                .createdAt(preferences.getCreatedAt())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }
}
