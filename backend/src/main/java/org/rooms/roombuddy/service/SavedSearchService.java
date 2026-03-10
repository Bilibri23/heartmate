package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.SavedSearchRequest;
import org.rooms.roombuddy.dto.response.SavedSearchResponse;
import org.rooms.roombuddy.entity.SavedSearch;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.SavedSearchRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedSearchService {
    
    private final SavedSearchRepository savedSearchRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public SavedSearchResponse createSavedSearch(UUID userId, SavedSearchRequest request) {
        log.info("Creating saved search for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        SavedSearch savedSearch = SavedSearch.builder()
                .user(user)
                .name(request.getName())
                .query(request.getQuery())
                .city(request.getCity())
                .neighborhood(request.getNeighborhood())
                .propertyType(request.getPropertyType())
                .minPrice(request.getMinPrice())
                .maxPrice(request.getMaxPrice())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .amenities(request.getAmenities())
                .maxDistance(request.getMaxDistance())
                .userLat(request.getUserLat())
                .userLon(request.getUserLon())
                .availableFrom(request.getAvailableFrom())
                .notifyNewListings(request.getNotifyNewListings())
                .notifyPriceDrops(request.getNotifyPriceDrops())
                .build();
        
        savedSearch = savedSearchRepository.save(savedSearch);
        log.info("Saved search created: {}", savedSearch.getId());
        
        return mapToResponse(savedSearch);
    }
    
    @Transactional(readOnly = true)
    public List<SavedSearchResponse> getUserSavedSearches(UUID userId) {
        log.info("Getting saved searches for user: {}", userId);
        
        List<SavedSearch> searches = savedSearchRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        return searches.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void deleteSavedSearch(UUID searchId, UUID userId) {
        log.info("Deleting saved search: {} for user: {}", searchId, userId);
        
        SavedSearch search = savedSearchRepository.findById(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved search not found"));
        
        if (!search.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Saved search not found");
        }
        
        savedSearchRepository.delete(search);
        log.info("Saved search deleted: {}", searchId);
    }
    
    @Transactional
    public SavedSearchResponse updateSavedSearch(UUID searchId, UUID userId, SavedSearchRequest request) {
        log.info("Updating saved search: {} for user: {}", searchId, userId);
        
        SavedSearch search = savedSearchRepository.findById(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved search not found"));
        
        if (!search.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Saved search not found");
        }
        
        search.setName(request.getName());
        search.setQuery(request.getQuery());
        search.setCity(request.getCity());
        search.setNeighborhood(request.getNeighborhood());
        search.setPropertyType(request.getPropertyType());
        search.setMinPrice(request.getMinPrice());
        search.setMaxPrice(request.getMaxPrice());
        search.setBedrooms(request.getBedrooms());
        search.setBathrooms(request.getBathrooms());
        search.setAmenities(request.getAmenities());
        search.setMaxDistance(request.getMaxDistance());
        search.setUserLat(request.getUserLat());
        search.setUserLon(request.getUserLon());
        search.setAvailableFrom(request.getAvailableFrom());
        search.setNotifyNewListings(request.getNotifyNewListings());
        search.setNotifyPriceDrops(request.getNotifyPriceDrops());
        
        search = savedSearchRepository.save(search);
        log.info("Saved search updated: {}", searchId);
        
        return mapToResponse(search);
    }
    
    @Transactional
    public void markSearchAsChecked(UUID searchId) {
        SavedSearch search = savedSearchRepository.findById(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved search not found"));
        
        search.setLastCheckedAt(LocalDateTime.now());
        savedSearchRepository.save(search);
    }
    
    private SavedSearchResponse mapToResponse(SavedSearch search) {
        return SavedSearchResponse.builder()
                .id(search.getId())
                .userId(search.getUser().getId())
                .name(search.getName())
                .query(search.getQuery())
                .city(search.getCity())
                .neighborhood(search.getNeighborhood())
                .propertyType(search.getPropertyType())
                .minPrice(search.getMinPrice())
                .maxPrice(search.getMaxPrice())
                .bedrooms(search.getBedrooms())
                .bathrooms(search.getBathrooms())
                .amenities(search.getAmenities())
                .maxDistance(search.getMaxDistance())
                .userLat(search.getUserLat())
                .userLon(search.getUserLon())
                .availableFrom(search.getAvailableFrom())
                .notifyNewListings(search.getNotifyNewListings())
                .notifyPriceDrops(search.getNotifyPriceDrops())
                .createdAt(search.getCreatedAt())
                .lastCheckedAt(search.getLastCheckedAt())
                .newListingsCount(0) // TODO: Calculate based on lastCheckedAt
                .build();
    }
}
