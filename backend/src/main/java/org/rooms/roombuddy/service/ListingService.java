package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.ListingApprovalRequest;
import org.rooms.roombuddy.dto.request.ListingRequest;
import org.rooms.roombuddy.dto.response.ListingResponse;
import org.rooms.roombuddy.dto.response.PhotoDTO;
import org.rooms.roombuddy.entity.ListingFavorite;
import org.rooms.roombuddy.entity.ListingPhoto;
import org.rooms.roombuddy.entity.PropertyListing;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.ListingFavoriteRepository;
import org.rooms.roombuddy.repository.ListingPhotoRepository;
import org.rooms.roombuddy.repository.PropertyListingRepository;
import org.rooms.roombuddy.repository.ReviewRepository;
import org.rooms.roombuddy.repository.ListingPreferencesRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListingService {
    
    private final PropertyListingRepository listingRepository;
    private final ListingPhotoRepository photoRepository;
    private final ListingFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final ListingPreferencesRepository preferencesRepository;
    
    @CacheEvict(value = "listings", allEntries = true)
    public ListingResponse createListing(UUID landlordId, ListingRequest request) {
        log.info("Creating listing for landlord: {}", landlordId);
        
        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with id: " + landlordId));
        
        // Verify user is a landlord
        if (landlord.getRole() != User.UserRole.LANDLORD) {
            throw new BadRequestException("Only landlords can create listings");
        }
        
        // Create listing
        PropertyListing listing = PropertyListing.builder()
                .landlord(landlord)
                .title(request.getTitle())
                .description(request.getDescription())
                .propertyType(parsePropertyType(request.getPropertyType()))
                .rentAmount(request.getRentAmount())
                .deposit(request.getDeposit())
                .agencyFees(request.getAgencyFees())
                .region(request.getRegion())
                .city(request.getCity())
                .neighborhood(request.getNeighborhood())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .distanceToUniversity(request.getDistanceToUniversity())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .squareMeters(request.getSquareMeters())
                .floor(request.getFloor())
                .amenities(request.getAmenities())
                .availableFrom(request.getAvailableFrom())
                .availableTo(request.getAvailableTo())
                .landlordWhatsapp(request.getLandlordWhatsapp())
                .status(parseStatus(request.getStatus()))
                .verified(false)
                .featured(false)
                .viewsCount(0)
                .favoritesCount(0)
                .build();
        
        // If status is PENDING or ACTIVE, set to PENDING for admin approval
        if (listing.getStatus() == PropertyListing.Status.PENDING || listing.getStatus() == PropertyListing.Status.ACTIVE) {
            listing.setStatus(PropertyListing.Status.PENDING);
        }
        
        PropertyListing saved = listingRepository.save(listing);
        log.info("Listing created successfully: {}", saved.getId());
        
        // Evict cache for new listing
        // CacheEvict annotation handles this automatically
        
        return mapToResponse(saved, null);
    }
    
    @Cacheable(value = "listings", key = "#listingId")
    @Transactional(readOnly = true)
    public ListingResponse getListing(UUID listingId, UUID userId) {
        log.info("Fetching listing: {}", listingId);
        
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        // Increment views count (async to not block cache)
        listing.setViewsCount(listing.getViewsCount() + 1);
        listingRepository.save(listing);
        
        return mapToResponse(listing, userId);
    }
    
    @CacheEvict(value = "listings", key = "#listingId")
    public ListingResponse updateListing(UUID listingId, UUID landlordId, ListingRequest request) {
        log.info("Updating listing: {} for landlord: {}", listingId, landlordId);
        
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        // Verify ownership
        if (!listing.getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("Only the listing owner can update it");
        }
        
        // Update fields
        if (request.getTitle() != null) {
            listing.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            listing.setDescription(request.getDescription());
        }
        if (request.getPropertyType() != null) {
            listing.setPropertyType(parsePropertyType(request.getPropertyType()));
        }
        if (request.getRentAmount() != null) {
            listing.setRentAmount(request.getRentAmount());
        }
        if (request.getDeposit() != null) {
            listing.setDeposit(request.getDeposit());
        }
        if (request.getAgencyFees() != null) {
            listing.setAgencyFees(request.getAgencyFees());
        }
        if (request.getRegion() != null) {
            listing.setRegion(request.getRegion());
        }
        if (request.getCity() != null) {
            listing.setCity(request.getCity());
        }
        if (request.getNeighborhood() != null) {
            listing.setNeighborhood(request.getNeighborhood());
        }
        if (request.getAddress() != null) {
            listing.setAddress(request.getAddress());
        }
        if (request.getLatitude() != null) {
            listing.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            listing.setLongitude(request.getLongitude());
        }
        if (request.getDistanceToUniversity() != null) {
            listing.setDistanceToUniversity(request.getDistanceToUniversity());
        }
        if (request.getBedrooms() != null) {
            listing.setBedrooms(request.getBedrooms());
        }
        if (request.getBathrooms() != null) {
            listing.setBathrooms(request.getBathrooms());
        }
        if (request.getSquareMeters() != null) {
            listing.setSquareMeters(request.getSquareMeters());
        }
        if (request.getFloor() != null) {
            listing.setFloor(request.getFloor());
        }
        if (request.getAmenities() != null) {
            listing.setAmenities(request.getAmenities());
        }
        if (request.getAvailableFrom() != null) {
            listing.setAvailableFrom(request.getAvailableFrom());
        }
        if (request.getAvailableTo() != null) {
            listing.setAvailableTo(request.getAvailableTo());
        }
        if (request.getLandlordWhatsapp() != null) {
            listing.setLandlordWhatsapp(request.getLandlordWhatsapp());
        }
        if (request.getStatus() != null) {
            PropertyListing.Status newStatus = parseStatus(request.getStatus());
            // If changing to ACTIVE, set to PENDING for admin approval
            if (newStatus == PropertyListing.Status.ACTIVE && listing.getStatus() != PropertyListing.Status.ACTIVE) {
                listing.setStatus(PropertyListing.Status.PENDING);
            } else {
                listing.setStatus(newStatus);
            }
        }
        
        PropertyListing updated = listingRepository.save(listing);
        log.info("Listing updated successfully: {}", listingId);
        
        return mapToResponse(updated, landlordId);
    }
    
    public void deleteListing(UUID listingId, UUID landlordId) {
        log.info("Deleting listing: {} by landlord: {}", listingId, landlordId);
        
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        // Verify ownership
        if (!listing.getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("Only the listing owner can delete it");
        }
        
        listingRepository.delete(listing);
        log.info("Listing deleted successfully: {}", listingId);
    }
    
    @Transactional(readOnly = true)
    public List<ListingResponse> searchListings(
            String city,
            String neighborhood,
            String propertyType,
            Integer minPrice,
            Integer maxPrice,
            List<String> amenities,
            UUID userId) {
        log.info("Searching listings with filters: city={}, neighborhood={}, type={}, minPrice={}, maxPrice={}", 
                city, neighborhood, propertyType, minPrice, maxPrice);
        
        // Start with base query for active and verified listings
        List<PropertyListing> listings = listingRepository.findByStatusAndVerified(
                PropertyListing.Status.ACTIVE, true);
        
        // Apply filters
        if (city != null && !city.isEmpty()) {
            listings = listings.stream()
                    .filter(listing -> city.equalsIgnoreCase(listing.getCity()))
                    .collect(Collectors.toList());
        }
        
        if (neighborhood != null && !neighborhood.isEmpty()) {
            listings = listings.stream()
                    .filter(listing -> neighborhood.equalsIgnoreCase(listing.getNeighborhood()))
                    .collect(Collectors.toList());
        }
        
        if (propertyType != null && !propertyType.isEmpty()) {
            try {
                PropertyListing.PropertyType type = PropertyListing.PropertyType.valueOf(propertyType.toUpperCase());
                listings = listings.stream()
                        .filter(listing -> listing.getPropertyType() == type)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid property type, return empty list
                return new ArrayList<>();
            }
        }
        
        if (minPrice != null) {
            listings = listings.stream()
                    .filter(listing -> listing.getRentAmount() != null && listing.getRentAmount() >= minPrice)
                    .collect(Collectors.toList());
        }
        
        if (maxPrice != null) {
            listings = listings.stream()
                    .filter(listing -> listing.getRentAmount() != null && listing.getRentAmount() <= maxPrice)
                    .collect(Collectors.toList());
        }
        
        // Filter by amenities
        if (amenities != null && !amenities.isEmpty()) {
            listings = listings.stream()
                    .filter(listing -> listing.getAmenities() != null && 
                            listing.getAmenities().containsAll(amenities))
                    .collect(Collectors.toList());
        }
        
        return listings.stream()
                .map(listing -> mapToResponse(listing, userId))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ListingResponse> getActiveListings(UUID userId) {
        log.info("Getting active listings for user: {}", userId);
        List<PropertyListing> listings = listingRepository.findActiveVerifiedListings();
        return listings.stream()
                .map(listing -> mapToResponse(listing, userId))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ListingResponse> getFeaturedListings(UUID userId) {
        log.info("Getting featured listings");
        List<PropertyListing> listings = listingRepository.findFeaturedListings();
        return listings.stream()
                .map(listing -> mapToResponse(listing, userId))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ListingResponse> getLandlordListings(UUID landlordId) {
        log.info("Getting listings for landlord: {}", landlordId);
        List<PropertyListing> listings = listingRepository.findByLandlordId(landlordId);
        return listings.stream()
                .map(listing -> mapToResponse(listing, landlordId))
                .collect(Collectors.toList());
    }
    
    public ListingResponse addPhoto(UUID listingId, UUID landlordId, String photoUrl, Boolean isPrimary) {
        log.info("Adding photo to listing: {}", listingId);
        
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        // Verify ownership
        if (!listing.getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("Only the listing owner can add photos");
        }
        
        // If this is primary, unset other primary photos
        if (Boolean.TRUE.equals(isPrimary)) {
            List<ListingPhoto> existingPhotos = photoRepository.findByListingId(listingId);
            for (ListingPhoto photo : existingPhotos) {
                if (photo.getIsPrimary()) {
                    photo.setIsPrimary(false);
                    photoRepository.save(photo);
                }
            }
        }
        
        // Get next display order
        List<ListingPhoto> existingPhotos = photoRepository.findByListingId(listingId);
        int nextOrder = existingPhotos.size();
        
        ListingPhoto photo = ListingPhoto.builder()
                .listing(listing)
                .photoUrl(photoUrl)
                .isPrimary(isPrimary != null ? isPrimary : false)
                .displayOrder(nextOrder)
                .build();
        
        photoRepository.save(photo);
        log.info("Photo added successfully to listing: {}", listingId);
        
        return mapToResponse(listing, landlordId);
    }
    
    public void removePhoto(UUID photoId, UUID landlordId) {
        log.info("Removing photo: {}", photoId);
        
        ListingPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found with id: " + photoId));
        
        // Verify ownership
        if (!photo.getListing().getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("Only the listing owner can remove photos");
        }
        
        photoRepository.delete(photo);
        log.info("Photo removed successfully: {}", photoId);
    }
    
    public ListingResponse toggleFavorite(UUID listingId, UUID userId) {
        log.info("Toggling favorite for listing: {} by user: {}", listingId, userId);
        
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        ListingFavorite favorite = favoriteRepository.findByUserIdAndListingId(userId, listingId).orElse(null);
        
        if (favorite != null) {
            // Remove favorite
            favoriteRepository.delete(favorite);
            listing.setFavoritesCount(Math.max(0, listing.getFavoritesCount() - 1));
            log.info("Favorite removed");
        } else {
            // Add favorite
            favorite = ListingFavorite.builder()
                    .user(userRepository.findById(userId).orElseThrow())
                    .listing(listing)
                    .build();
            favoriteRepository.save(favorite);
            listing.setFavoritesCount(listing.getFavoritesCount() + 1);
            log.info("Favorite added");
        }
        
        listingRepository.save(listing);
        return mapToResponse(listing, userId);
    }
    
    @Transactional(readOnly = true)
    public List<ListingResponse> getFavorites(UUID userId) {
        log.info("Getting favorites for user: {}", userId);
        List<ListingFavorite> favorites = favoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(favorite -> mapToResponse(favorite.getListing(), userId))
                .collect(Collectors.toList());
    }
    
    public ListingResponse approveOrRejectListing(UUID listingId, UUID adminId, ListingApprovalRequest request) {
        log.info("Admin {} approving/rejecting listing: {}", adminId, listingId);
        
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));
        
        // Check if admin has ADMIN role
        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new BadRequestException("Only admins can approve/reject listings");
        }
        
        // Validate status - accept ACTIVE for approval, REJECTED string maps to INACTIVE
        PropertyListing.Status status;
        String statusUpper = request.getStatus().toUpperCase();
        
        if ("ACTIVE".equals(statusUpper)) {
            status = PropertyListing.Status.ACTIVE;
        } else if ("REJECTED".equals(statusUpper)) {
            status = PropertyListing.Status.INACTIVE;
            
            // Validate rejection reason
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BadRequestException("Rejection reason is required when rejecting listing");
            }
        } else {
            throw new BadRequestException("Invalid status: " + request.getStatus() + ". Must be ACTIVE or REJECTED");
        }
        
        // Update listing
        listing.setStatus(status);
        listing.setVerified(status == PropertyListing.Status.ACTIVE);
        listing.setRejectionReason(status == PropertyListing.Status.INACTIVE ? request.getRejectionReason() : null);
        listing.setVerifiedBy(admin);
        if (status == PropertyListing.Status.ACTIVE) {
            listing.setVerifiedAt(java.time.LocalDateTime.now());
        } else {
            listing.setVerifiedAt(null);
        }
        
        // Set featured if provided
        if (request.getFeatured() != null) {
            listing.setFeatured(request.getFeatured());
        }
        
        PropertyListing updated = listingRepository.save(listing);
        log.info("Listing {} {} by admin {}", listingId, status, adminId);
        
        // Send notification to landlord
        if (status == PropertyListing.Status.ACTIVE) {
            notificationService.createNotification(
                    listing.getLandlord().getId(),
                    org.rooms.roombuddy.entity.Notification.NotificationType.LISTING_APPROVED,
                    "Listing Approved!",
                    "Your listing '" + listing.getTitle() + "' has been approved and is now live.",
                    listingId,
                    "LISTING",
                    "/admin/landlord/listings"
            );
        } else {
            notificationService.createNotification(
                    listing.getLandlord().getId(),
                    org.rooms.roombuddy.entity.Notification.NotificationType.LISTING_REJECTED,
                    "Listing Rejected",
                    "Your listing '" + listing.getTitle() + "' was rejected: " + request.getRejectionReason(),
                    listingId,
                    "LISTING",
                    "/admin/landlord/listings"
            );
        }
        
        return mapToResponse(updated, null);
    }
    
    @Transactional(readOnly = true)
    public List<ListingResponse> getPendingListings() {
        log.info("Fetching pending listings");
        List<PropertyListing> listings = listingRepository.findPendingListings();
        return listings.stream()
                .map(listing -> mapToResponse(listing, null))
                .collect(Collectors.toList());
    }
    
    private PropertyListing.PropertyType parsePropertyType(String propertyType) {
        if (propertyType == null) {
            return null;
        }
        try {
            return PropertyListing.PropertyType.valueOf(propertyType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid property type: " + propertyType);
        }
    }
    
    private PropertyListing.Status parseStatus(String status) {
        if (status == null) {
            return PropertyListing.Status.DRAFT;
        }
        try {
            return PropertyListing.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }
    }
    
    private ListingResponse mapToResponse(PropertyListing listing, UUID userId) {
        List<ListingPhoto> photos = photoRepository.findByListingIdOrderByDisplayOrderAsc(listing.getId());
        List<PhotoDTO> photoDTOs = photos.stream()
                .map(photo -> PhotoDTO.builder()
                        .id(photo.getId())
                        .photoUrl(photo.getPhotoUrl())
                        .isPrimary(photo.getIsPrimary())
                        .displayOrder(photo.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());
        ListingPhoto primaryPhoto = photos.stream().filter(ListingPhoto::getIsPrimary).findFirst().orElse(null);
        if (primaryPhoto == null && !photos.isEmpty()) {
            primaryPhoto = photos.get(0);
        }
        
        Boolean isFavorite = userId != null && favoriteRepository.existsByUserIdAndListingId(userId, listing.getId());
        
        // Get review stats for listing
        Double averageRating = reviewRepository.getAverageRatingForListing(listing.getId());
        long reviewCount = reviewRepository.countReviewsForListing(listing.getId());
        
        // Calculate compatibility score for students viewing listings
        Integer compatibilityScore = null;
        String compatibilityReason = null;
        if (userId != null) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getRole() == User.UserRole.STUDENT) {
                    var compatibility = calculateListingCompatibility(userId, listing);
                    compatibilityScore = compatibility.getScore();
                    compatibilityReason = compatibility.getReason();
                }
            } catch (Exception e) {
                log.debug("Could not calculate compatibility score: {}", e.getMessage());
            }
        }
        
        return ListingResponse.builder()
                .id(listing.getId())
                .landlordId(listing.getLandlord().getId())
                .landlordName(listing.getLandlord().getFirstName() + " " + listing.getLandlord().getLastName())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .propertyType(listing.getPropertyType() != null ? listing.getPropertyType().name() : null)
                .rentAmount(listing.getRentAmount())
                .deposit(listing.getDeposit())
                .agencyFees(listing.getAgencyFees())
                .region(listing.getRegion())
                .city(listing.getCity())
                .neighborhood(listing.getNeighborhood())
                .address(listing.getAddress())
                .latitude(listing.getLatitude())
                .longitude(listing.getLongitude())
                .distanceToUniversity(listing.getDistanceToUniversity())
                .bedrooms(listing.getBedrooms())
                .bathrooms(listing.getBathrooms())
                .squareMeters(listing.getSquareMeters())
                .floor(listing.getFloor())
                .amenities(listing.getAmenities())
                .availableFrom(listing.getAvailableFrom())
                .availableTo(listing.getAvailableTo())
                .status(listing.getStatus().name())
                .verified(listing.getVerified())
                .featured(listing.getFeatured())
                .viewsCount(listing.getViewsCount())
                .favoritesCount(listing.getFavoritesCount())
                .photos(photoDTOs)
                .primaryPhotoUrl(primaryPhoto != null ? primaryPhoto.getPhotoUrl() : null)
                .isFavorite(isFavorite)
                .averageRating(averageRating)
                .reviewCount((int) reviewCount)
                .compatibilityScore(compatibilityScore)
                .compatibilityReason(compatibilityReason)
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }

    /**
     * Calculate compatibility score between a student's preferences and a listing
     */
    private CompatibilityResult calculateListingCompatibility(UUID studentId, PropertyListing listing) {
        try {
            var prefsOpt = preferencesRepository.findByUserId(studentId);
            if (prefsOpt.isEmpty()) {
                return new CompatibilityResult(0, null);
            }
            
            var prefs = prefsOpt.get();
            List<String> reasons = new ArrayList<>();
            int score = 0;
            int factors = 0;
            
            // Budget match (40% weight)
            if (prefs.getMinBudget() != null && prefs.getMaxBudget() != null && listing.getRentAmount() != null) {
                int rent = listing.getRentAmount();
                if (rent >= prefs.getMinBudget() && rent <= prefs.getMaxBudget()) {
                    score += 40;
                    reasons.add("within your budget");
                } else if (rent < prefs.getMinBudget()) {
                    score += 35;
                    reasons.add("below your budget");
                } else {
                    int overBy = rent - prefs.getMaxBudget();
                    int budgetRange = prefs.getMaxBudget() - prefs.getMinBudget();
                    if (budgetRange > 0) {
                        int penalty = Math.min(20, (overBy * 20) / budgetRange);
                        score += Math.max(0, 40 - penalty);
                    }
                }
                factors++;
            }
            
            // Location match (30% weight)
            if (prefs.getPreferredLocations() != null && !prefs.getPreferredLocations().isEmpty()) {
                boolean matchesLocation = prefs.getPreferredLocations().stream()
                    .anyMatch(loc -> loc.equalsIgnoreCase(listing.getCity()) || 
                                   loc.equalsIgnoreCase(listing.getNeighborhood()));
                if (matchesLocation) {
                    score += 30;
                    reasons.add("in your preferred location");
                } else {
                    score += 10;
                }
                factors++;
            }
            
            // Distance to university (20% weight)
            if (prefs.getMaxDistanceFromCampus() != null && listing.getDistanceToUniversity() != null) {
                int comparisonResult = listing.getDistanceToUniversity().compareTo(prefs.getMaxDistanceFromCampus());
                if (comparisonResult <= 0) {  // listing distance <= max preferred distance
                    score += 20;
                    reasons.add("close to campus");
                } else {
                    score += 5;
                }
                factors++;
            }

            // Property type (10% weight)
            if (prefs.getPropertyTypes() != null && !prefs.getPropertyTypes().isEmpty() && listing.getPropertyType() != null) {
                boolean matchesType = prefs.getPropertyTypes().stream()
                    .anyMatch(type -> type.equalsIgnoreCase(listing.getPropertyType().name()));
                if (matchesType) {
                    score += 10;
                    reasons.add("matches your preferred property type");
                } else {
                    score += 2;
                }
                factors++;
            }
            
            // Rebalancing: Budget=40, Location=30, Distance=20, Type=10 (total=100)
            
            
            // Normalize score
            if (factors > 0) {
                score = Math.min(100, score);
            }
            
            String reason = reasons.isEmpty() ? null : "Matches: " + String.join(", ", reasons);
            return new CompatibilityResult(score, reason);
        } catch (Exception e) {
            log.error("Error calculating listing compatibility: {}", e.getMessage());
            return new CompatibilityResult(0, null);
        }
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class CompatibilityResult {
        private int score;
        private String reason;
    }
    
    // Helper method to find listing by ID and verify landlord ownership
    private PropertyListing findListingByIdAndLandlord(UUID listingId, UUID landlordId) {
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
        if (!listing.getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("Only the listing owner can perform this action");
        }
        
        return listing;
    }

    // Mark listing as rented
    public ListingResponse markAsRented(UUID listingId, UUID landlordId) {
        PropertyListing listing = findListingByIdAndLandlord(listingId, landlordId);
        listing.setStatus(PropertyListing.Status.RENTED);
        PropertyListing saved = listingRepository.save(listing);
        return mapToResponse(saved, landlordId);
    }

    // Mark listing as available - if DRAFT, set to PENDING for admin approval
    public ListingResponse markAsAvailable(UUID listingId, UUID landlordId) {
        PropertyListing listing = findListingByIdAndLandlord(listingId, landlordId);
        // If listing is DRAFT or INACTIVE, set to PENDING for admin approval
        if (listing.getStatus() == PropertyListing.Status.DRAFT || 
            listing.getStatus() == PropertyListing.Status.INACTIVE) {
            listing.setStatus(PropertyListing.Status.PENDING);
            log.info("Listing {} set to PENDING for admin approval", listingId);
        } else {
            // For RENTED listings being re-listed, set directly to ACTIVE
            listing.setStatus(PropertyListing.Status.ACTIVE);
        }
        PropertyListing saved = listingRepository.save(listing);
        return mapToResponse(saved, landlordId);
    }

    // Track view
    public void trackView(UUID listingId, UUID userId) {
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listing.setViewsCount(listing.getViewsCount() + 1);
        listingRepository.save(listing);
    }

    // Get statistics
    public Map<String, Object> getLandlordStatistics(UUID landlordId) {
        List<PropertyListing> listings = listingRepository.findByLandlordId(landlordId);
        return Map.of(
                "totalListings", listings.size(),
                "activeListings", listings.stream().filter(l -> l.getStatus() == PropertyListing.Status.ACTIVE).count(),
                "rentedListings", listings.stream().filter(l -> l.getStatus() == PropertyListing.Status.RENTED).count(),
                "totalViews", listings.stream().mapToInt(PropertyListing::getViewsCount).sum()
        );
    }

    // Update search to support pagination
    public Page<ListingResponse> searchListings(String city, String neighborhood,
                                                String propertyType, Integer minPrice, Integer maxPrice,
                                                List<String> amenities, UUID userId, Pageable pageable) {
    log.info("Searching listings with pagination: city={}, neighborhood={}, type={}, page={}, size={}", 
            city, neighborhood, propertyType, pageable.getPageNumber(), pageable.getPageSize());
    
    // Get all active and verified listings first
    List<PropertyListing> allListings = listingRepository.findByStatusAndVerified(
            PropertyListing.Status.ACTIVE, true);
    
    // Apply filters
    if (city != null && !city.isEmpty()) {
        allListings = allListings.stream()
                .filter(listing -> city.equalsIgnoreCase(listing.getCity()))
                .collect(Collectors.toList());
    }
    
    if (neighborhood != null && !neighborhood.isEmpty()) {
        allListings = allListings.stream()
                .filter(listing -> neighborhood.equalsIgnoreCase(listing.getNeighborhood()))
                .collect(Collectors.toList());
    }
    
    if (propertyType != null && !propertyType.isEmpty()) {
        try {
            PropertyListing.PropertyType type = PropertyListing.PropertyType.valueOf(propertyType.toUpperCase());
            allListings = allListings.stream()
                    .filter(listing -> listing.getPropertyType() == type)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            // Invalid property type, return empty page
            return Page.empty(pageable);
        }
    }
    
    if (minPrice != null) {
        allListings = allListings.stream()
                .filter(listing -> listing.getRentAmount() != null && listing.getRentAmount() >= minPrice)
                .collect(Collectors.toList());
    }
    
    if (maxPrice != null) {
        allListings = allListings.stream()
                .filter(listing -> listing.getRentAmount() != null && listing.getRentAmount() <= maxPrice)
                .collect(Collectors.toList());
    }
    
    if (amenities != null && !amenities.isEmpty()) {
        allListings = allListings.stream()
                .filter(listing -> listing.getAmenities() != null && 
                        listing.getAmenities().containsAll(amenities))
                .collect(Collectors.toList());
    }
    
    // Apply sorting
    if (pageable.getSort().isSorted()) {
        allListings = allListings.stream()
                .sorted((l1, l2) -> {
                    // Default sort by createdAt DESC
                    return l2.getCreatedAt().compareTo(l1.getCreatedAt());
                })
                .collect(Collectors.toList());
    }
    
    // Calculate pagination
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), allListings.size());
    
    List<PropertyListing> pageContent = allListings.subList(start, end);
    List<ListingResponse> responses = pageContent.stream()
            .map(listing -> mapToResponse(listing, userId))
            .collect(Collectors.toList());
    
    return new org.springframework.data.domain.PageImpl<>(
            responses, pageable, allListings.size());
}
}

