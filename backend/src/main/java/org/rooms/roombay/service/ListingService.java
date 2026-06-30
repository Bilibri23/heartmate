package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.ArMarkersUpdateRequest;
import org.rooms.roombay.dto.request.ListingApprovalRequest;
import org.rooms.roombay.dto.request.ListingRequest;
import org.rooms.roombay.dto.response.AreaStatsResponse;
import org.rooms.roombay.dto.response.ArMarkerResponse;
import org.rooms.roombay.dto.response.ListingRecommendationResponse;
import org.rooms.roombay.dto.response.ListingResponse;
import org.rooms.roombay.dto.response.PhotoDTO;
import org.rooms.roombay.entity.LandlordVerification;
import org.rooms.roombay.entity.ListingArMarker;
import org.rooms.roombay.entity.ListingFavorite;
import org.rooms.roombay.entity.ListingPhoto;
import org.rooms.roombay.entity.ListingSearchOutbox;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.ListingArMarkerRepository;
import org.rooms.roombay.repository.ListingFavoriteRepository;
import org.rooms.roombay.repository.ListingPhotoRepository;
import org.rooms.roombay.repository.ListingPreferencesRepository;
import org.rooms.roombay.repository.ListingSearchOutboxRepository;
import org.rooms.roombay.repository.ListingSpecifications;
import org.rooms.roombay.repository.ListingViewRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.ReviewRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.UserRepository;
import org.rooms.roombay.security.SecurityUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListingService {

    private static final int MAX_LISTING_PHOTOS = 15;
    
    private final PropertyListingRepository listingRepository;
    private final ListingPhotoRepository photoRepository;
    private final ListingFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final ListingPreferencesRepository preferencesRepository;
    private final RoomApplicationRepository applicationRepository;
    private final ListingSearchOutboxRepository listingSearchOutboxRepository;
    private final SecurityAuditService securityAuditService;
    private final LandlordVerificationRepository landlordVerificationRepository;
    private final ListingViewRepository listingViewRepository;
    private final ListingArMarkerRepository listingArMarkerRepository;
    private final PlatformSettingsService platformSettingsService;
    private final AnalyticsEventService analyticsEventService;
    private final AuditLogService auditLogService;
    
    @CacheEvict(value = "listings", allEntries = true)
    public ListingResponse createListing(UUID landlordId, ListingRequest request) {
        log.info("Creating listing for landlord: {}", landlordId);
        
        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with id: " + landlordId));
        
        // Verify user is a landlord
        if (landlord.getRole() != User.UserRole.LANDLORD) {
            throw new BadRequestException("Only landlords can create listings");
        }

        // Enforce max listings per landlord
        int maxListings = platformSettingsService.getRawSettings().getMaxListingsPerLandlord();
        long currentListings = listingRepository.countByLandlordId(landlordId);
        if (currentListings >= maxListings) {
            throw new BadRequestException("You have reached the maximum allowed listings (" + maxListings + "). Please contact support.");
        }

        List<PropertyListing> recentDuplicates = listingRepository.findRecentEquivalentDraftOrPending(
                landlordId,
                request.getTitle(),
                request.getRentAmount(),
                request.getCity(),
                request.getNeighborhood(),
                request.getAddress(),
                LocalDateTime.now().minusMinutes(15));
        if (!recentDuplicates.isEmpty()) {
            PropertyListing existing = recentDuplicates.get(0);
            log.warn("Duplicate listing create suppressed for landlord={} existingListing={}", landlordId, existing.getId());
            return mapToResponse(existing, landlordId);
        }

        validateListingPricing(request, true);
        
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
                .isUnfurnished(Boolean.TRUE.equals(request.getIsUnfurnished()))
                .roomPreviewEnabled(Boolean.TRUE.equals(request.getRoomPreviewEnabled()))
                .roomLengthMeters(request.getRoomLengthMeters())
                .roomWidthMeters(request.getRoomWidthMeters())
                .roomHeightMeters(request.getRoomHeightMeters())
                .roomPreviewPhotoUrl(null)
                .roomPreviewStatus(Boolean.TRUE.equals(request.getRoomPreviewEnabled())
                        ? PropertyListing.RoomPreviewStatus.PENDING_REVIEW
                        : PropertyListing.RoomPreviewStatus.NOT_ENABLED)
                .videoTourUrl(request.getVideoTourUrl())
                .videoTourEmbedCode(request.getVideoTourEmbedCode())
                .virtualTourProvider(request.getVirtualTourProvider())
                .videoTourThumbnail(request.getVideoTourThumbnail())
                .videoTourDuration(request.getVideoTourDuration())
                .availableFrom(request.getAvailableFrom())
                .availableTo(request.getAvailableTo())
                .landlordWhatsapp(request.getLandlordWhatsapp())
                .status(parseStatus(request.getStatus()))
                .verified(false)
                .featured(false)
                .viewsCount(0)
                .favoritesCount(0)
                .build();

        applyTaxonomyFields(listing, request, true);

        if (Boolean.TRUE.equals(listing.getRoomPreviewEnabled())) {
            throw new BadRequestException("Create the listing photos before enabling Room Preview");
        }
        
        // If status is PENDING or ACTIVE, set to PENDING for admin approval (unless auto-approve is on)
        if (listing.getStatus() == PropertyListing.Status.PENDING || listing.getStatus() == PropertyListing.Status.ACTIVE) {
            boolean autoApprove = Boolean.TRUE.equals(platformSettingsService.getRawSettings().getAutoApproveListings());
            listing.setStatus(autoApprove ? PropertyListing.Status.ACTIVE : PropertyListing.Status.PENDING);
            if (autoApprove) {
                listing.setVerified(true);
            }
        }
        
        PropertyListing saved = listingRepository.save(listing);
        log.info("Listing created successfully: {}", saved.getId());
        securityAuditService.logAction("listing.create", landlordId, saved.getId(), "LISTING");
        enqueueSearchOutbox(saved, ListingSearchOutbox.EVENT_CREATED);
        
        // Evict cache for new listing
        // CacheEvict annotation handles this automatically
        
        return mapToResponse(saved, null);
    }
    
    @Transactional(readOnly = true)
    public ListingResponse getListing(UUID listingId, UUID userId) {
        log.info("Fetching listing: {}", listingId);
        
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        
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
        if (request.getIsUnfurnished() != null) {
            listing.setIsUnfurnished(request.getIsUnfurnished());
        }
        if (request.getRoomLengthMeters() != null) {
            listing.setRoomLengthMeters(request.getRoomLengthMeters());
        }
        if (request.getRoomWidthMeters() != null) {
            listing.setRoomWidthMeters(request.getRoomWidthMeters());
        }
        if (request.getRoomHeightMeters() != null) {
            listing.setRoomHeightMeters(request.getRoomHeightMeters());
        }
        if (request.getRoomPreviewPhotoUrl() != null) {
            String photoUrl = blankToNull(request.getRoomPreviewPhotoUrl());
            if (photoUrl != null) {
                assertListingPhotoUrl(listing.getId(), photoUrl);
            }
            listing.setRoomPreviewPhotoUrl(photoUrl);
        }
        if (request.getRoomPreviewEnabled() != null) {
            listing.setRoomPreviewEnabled(request.getRoomPreviewEnabled());
            if (Boolean.TRUE.equals(request.getRoomPreviewEnabled())) {
                String previewUrl = blankToNull(request.getRoomPreviewPhotoUrl()) != null
                        ? blankToNull(request.getRoomPreviewPhotoUrl())
                        : blankToNull(listing.getRoomPreviewPhotoUrl());
                if (previewUrl == null) {
                    throw new BadRequestException("Room Preview requires a listing photo");
                }
                assertListingPhotoUrl(listing.getId(), previewUrl);
                listing.setRoomPreviewPhotoUrl(previewUrl);
                if (listing.getRoomPreviewStatus() == null
                        || listing.getRoomPreviewStatus() == PropertyListing.RoomPreviewStatus.NOT_ENABLED) {
                    listing.setRoomPreviewStatus(PropertyListing.RoomPreviewStatus.PENDING_REVIEW);
                }
                analyticsEventService.emit("room_preview_enabled_by_landlord", landlordId, "LANDLORD", listingId,
                        Map.of("hasDimensions", hasRoomDimensions(listing)));
            } else {
                listing.setRoomPreviewStatus(PropertyListing.RoomPreviewStatus.NOT_ENABLED);
            }
        }
        if (request.getRoomPreviewStatus() != null && "ADMIN".equalsIgnoreCase(SecurityUtils.getCurrentUserRole())) {
            listing.setRoomPreviewStatus(parseRoomPreviewStatus(request.getRoomPreviewStatus()));
        }
        if (request.getVideoTourUrl() != null) {
            listing.setVideoTourUrl(request.getVideoTourUrl());
        }
        if (request.getVideoTourEmbedCode() != null) {
            listing.setVideoTourEmbedCode(request.getVideoTourEmbedCode());
        }
        if (request.getVirtualTourProvider() != null) {
            listing.setVirtualTourProvider(request.getVirtualTourProvider());
        }
        if (request.getVideoTourThumbnail() != null) {
            listing.setVideoTourThumbnail(request.getVideoTourThumbnail());
        }
        if (request.getVideoTourDuration() != null) {
            listing.setVideoTourDuration(request.getVideoTourDuration());
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

        applyTaxonomyFields(listing, request, false);
        
        PropertyListing updated = listingRepository.save(listing);
        log.info("Listing updated successfully: {}", listingId);
        securityAuditService.logAction("listing.update", landlordId, listingId, "LISTING");
        enqueueSearchOutbox(updated, ListingSearchOutbox.EVENT_UPDATED);
        
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
        enqueueSearchOutbox(listing, ListingSearchOutbox.EVENT_DELETED);
        securityAuditService.logAction("listing.delete", landlordId, listingId, "LISTING");
        listingRepository.delete(listing);
        log.info("Listing deleted successfully: {}", listingId);
    }
    
    /** Enqueues all ACTIVE listings for search index (used when ES index is empty on startup). */
    @Transactional
    public int bulkEnqueueActiveListingsForSearch() {
        List<PropertyListing> active = listingRepository.findByStatusAndVerified(
                PropertyListing.Status.ACTIVE, true);
        for (PropertyListing listing : active) {
            enqueueSearchOutbox(listing, ListingSearchOutbox.EVENT_UPDATED);
        }
        log.info("Enqueued {} ACTIVE listings for search index", active.size());
        return active.size();
    }

    /** Builds the search-index payload and appends a row to the transactional outbox. */
    private void enqueueSearchOutbox(PropertyListing listing, String eventType) {
        Map<String, Object> payload = buildSearchPayload(listing);
        ListingSearchOutbox outbox = ListingSearchOutbox.builder()
                .listingId(listing.getId())
                .eventType(eventType)
                .payload(payload)
                .build();
        listingSearchOutboxRepository.save(outbox);
    }
    
    /** Snapshot of a listing for the search index (id, title, description, city, neighborhood, amenities, furnished, price, propertyType, location, verified, featured). */
    private static Map<String, Object> buildSearchPayload(PropertyListing l) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", l.getId() != null ? l.getId().toString() : null);
        map.put("title", l.getTitle());
        map.put("description", l.getDescription());
        map.put("city", l.getCity());
        map.put("neighborhood", l.getNeighborhood());
        map.put("amenities", l.getAmenities());
        map.put("furnished", l.getAmenities() != null && l.getAmenities().contains("Furnished"));
        map.put("price", l.getRentAmount());
        map.put("propertyType", l.getPropertyType() != null ? l.getPropertyType().name() : null);
        map.put("bedrooms", l.getBedrooms());
        map.put("bathrooms", l.getBathrooms());
        if (l.getLatitude() != null && l.getLongitude() != null) {
            map.put("latitude", l.getLatitude().doubleValue());
            map.put("longitude", l.getLongitude().doubleValue());
        }
        map.put("verified", Boolean.TRUE.equals(l.getVerified()));
        map.put("featured", Boolean.TRUE.equals(l.getFeatured()));
        map.put("status", l.getStatus() != null ? l.getStatus().name() : null);
        if (l.getCreatedAt() != null) {
            map.put("createdAt", l.getCreatedAt().toString());
        }
        map.put("viewsCount", l.getViewsCount() != null ? l.getViewsCount() : 0);
        return map;
    }
    
    @Transactional(readOnly = true)
    public Page<ListingResponse> searchListingsAdvanced(
            String query,
            String city,
            String neighborhood,
            String propertyType,
            Integer minPrice,
            Integer maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            List<String> amenities,
            Double maxDistance,
            Double userLat,
            Double userLon,
            String availableFrom,
            UUID userId,
            Pageable pageable) {
        return searchListingsAdvanced(
                query, city, neighborhood, propertyType, minPrice, maxPrice,
                bedrooms, bathrooms, amenities, maxDistance, userLat, userLon,
                null, null, null, null,
                availableFrom, null, null, null, null, userId, pageable);
    }

    public Page<ListingResponse> searchListingsAdvanced(
            String query,
            String city,
            String neighborhood,
            String propertyType,
            Integer minPrice,
            Integer maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            List<String> amenities,
            Double maxDistance,
            Double userLat,
            Double userLon,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            String availableFrom,
            String listingPurpose,
            String stayType,
            String furnishingStatus,
            Boolean sharedAccommodation,
            UUID userId,
            Pageable pageable) {
        log.info("Advanced search - query: {}, city: {}, bedrooms: {}, bathrooms: {}, maxDistance: {}, geo: {}",
                query, city, bedrooms, bathrooms, maxDistance, (minLat != null || userLat != null));

        // On the DB fallback (Elasticsearch handles exact geo in prod) we approximate the "Near me"
        // radius with a bounding box, and use the viewport box directly for "search this area".
        Double[] box = resolveBoundingBox(minLat, maxLat, minLng, maxLng, userLat, userLon, maxDistance);

        var spec = ListingSpecifications.buildSearchSpec(
                query, city, neighborhood, propertyType,
                minPrice, maxPrice, bedrooms, bathrooms,
                amenities, availableFrom,
                listingPurpose, stayType, furnishingStatus, sharedAccommodation,
                box[0], box[1], box[2], box[3]);
        Page<PropertyListing> page = listingRepository.findAll(spec, pageable);
        return page.map(listing -> mapToResponse(listing, userId));
    }

    /**
     * Resolve an effective bounding box: prefer an explicit viewport box; otherwise derive one from
     * the "Near me" centre + radius (km converted to degrees). Returns {minLat, maxLat, minLng,
     * maxLng}, all null when no geo constraint applies.
     */
    private static Double[] resolveBoundingBox(Double minLat, Double maxLat, Double minLng, Double maxLng,
                                               Double centerLat, Double centerLng, Double radiusKm) {
        if (minLat != null && maxLat != null && minLng != null && maxLng != null) {
            return new Double[]{minLat, maxLat, minLng, maxLng};
        }
        if (centerLat != null && centerLng != null && radiusKm != null && radiusKm > 0) {
            double latDelta = radiusKm / 111.0;
            double cos = Math.abs(Math.cos(Math.toRadians(centerLat)));
            double lngDelta = radiusKm / (111.320 * (cos < 1e-6 ? 1e-6 : cos));
            return new Double[]{centerLat - latDelta, centerLat + latDelta,
                    centerLng - lngDelta, centerLng + lngDelta};
        }
        return new Double[]{null, null, null, null};
    }

    /** Per-neighborhood rent stats for the map heatmap overlay (optional city filter). */
    @Transactional(readOnly = true)
    public List<AreaStatsResponse> areaRentStats(String city) {
        String filter = (city == null || city.isBlank()) ? null : city;
        return listingRepository.aggregateRentByNeighborhood(filter).stream()
                .map(r -> AreaStatsResponse.builder()
                        .city((String) r[0])
                        .neighborhood((String) r[1])
                        .listingCount(r[2] == null ? 0L : ((Number) r[2]).longValue())
                        .avgRent(r[3] == null ? null : ((Number) r[3]).doubleValue())
                        .minRent(r[4] == null ? null : ((Number) r[4]).intValue())
                        .maxRent(r[5] == null ? null : ((Number) r[5]).intValue())
                        .build())
                .toList();
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
                .map(listing -> {
                    ListingResponse r = mapToResponse(listing, landlordId);
                    r.setApplicationsCount((int) applicationRepository.countByListingId(listing.getId()));
                    return r;
                })
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

        List<ListingPhoto> existingPhotos = photoRepository.findByListingId(listingId);
        if (existingPhotos.size() >= MAX_LISTING_PHOTOS) {
            throw new BadRequestException("Maximum of " + MAX_LISTING_PHOTOS + " photos per listing");
        }
        
        // If this is primary, unset other primary photos
        if (Boolean.TRUE.equals(isPrimary)) {
            for (ListingPhoto photo : existingPhotos) {
                if (photo.getIsPrimary()) {
                    photo.setIsPrimary(false);
                    photoRepository.save(photo);
                }
            }
        }
        
        // Get next display order
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
        List<ListingFavorite> favorites = favoriteRepository.findByUserIdWithListingAndLandlord(userId);
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
        if (request.getRoomPreviewStatus() != null) {
            listing.setRoomPreviewStatus(parseRoomPreviewStatus(request.getRoomPreviewStatus()));
        }
        
        PropertyListing updated = listingRepository.save(listing);
        log.info("Listing {} {} by admin {}", listingId, status, adminId);
        securityAuditService.logAction("admin.listing.review", adminId, listingId, "LISTING");
        auditLogService.logAdminAction(
                adminId,
                "LISTING_" + (status == PropertyListing.Status.ACTIVE ? "APPROVED" : "REJECTED"),
                "Listing",
                listingId,
                "Listing review. status=" + status.name() + ", reason=" + request.getRejectionReason()
        );
        analyticsEventService.emit(
                status == PropertyListing.Status.ACTIVE ? "listing_approved" : "listing_rejected",
                adminId,
                "ADMIN",
                listingId,
                Map.of("landlordId", listing.getLandlord().getId().toString())
        );
        
        // Re-index search so approved/rejected listings appear in search (ES filters by ACTIVE)
        enqueueSearchOutbox(updated, ListingSearchOutbox.EVENT_UPDATED);
        
        // Send notification to landlord
        if (status == PropertyListing.Status.ACTIVE) {
            notificationService.createNotification(
                    listing.getLandlord().getId(),
                    org.rooms.roombay.entity.Notification.NotificationType.LISTING_APPROVED,
                    "Listing Approved!",
                    "Your listing '" + listing.getTitle() + "' has been approved and is now live.",
                    listingId,
                    "LISTING",
                    "/admin/landlord/listings"
            );
        } else {
            notificationService.createNotification(
                    listing.getLandlord().getId(),
                    org.rooms.roombay.entity.Notification.NotificationType.LISTING_REJECTED,
                    "Listing Rejected",
                    "Your listing '" + listing.getTitle() + "' was rejected: " + request.getRejectionReason(),
                    listingId,
                    "LISTING",
                    "/admin/landlord/listings"
            );
        }
        
        return mapToResponse(updated, null);
    }

    public ListingResponse updateRoomPreviewStatus(UUID listingId, UUID adminId, String roomPreviewStatus) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));
        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new BadRequestException("Only admins can update Room Preview status");
        }
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
        listing.setRoomPreviewStatus(parseRoomPreviewStatus(roomPreviewStatus));
        PropertyListing saved = listingRepository.save(listing);
        auditLogService.logAdminAction(
                adminId,
                "ROOM_PREVIEW_STATUS_UPDATED",
                "Listing",
                listingId,
                "Room Preview status=" + saved.getRoomPreviewStatus()
        );
        return mapToResponse(saved, null);
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

    private void validateListingPricing(ListingRequest request, boolean isCreate) {
        PropertyListing.ListingPurpose purpose = parseListingPurpose(request.getListingPurpose());
        if (purpose == null) {
            purpose = PropertyListing.ListingPurpose.RENT;
        }
        if (purpose == PropertyListing.ListingPurpose.SALE) {
            Integer salePrice = request.getSalePrice() != null ? request.getSalePrice() : request.getRentAmount();
            if (isCreate && salePrice == null) {
                throw new BadRequestException("Sale price is required for sale listings");
            }
        } else if (isCreate && request.getRentAmount() == null) {
            throw new BadRequestException("Rent amount is required for rental listings");
        }
    }

    private void applyTaxonomyFields(PropertyListing listing, ListingRequest request, boolean isCreate) {
        if (request.getListingPurpose() != null) {
            listing.setListingPurpose(parseListingPurpose(request.getListingPurpose()));
        } else if (isCreate) {
            listing.setListingPurpose(PropertyListing.ListingPurpose.RENT);
        }

        if (request.getStayType() != null) {
            listing.setStayType(parseStayType(request.getStayType()));
        } else if (isCreate) {
            listing.setStayType(PropertyListing.StayType.LONG_TERM);
        }

        if (request.getPricePeriod() != null) {
            listing.setPricePeriod(parsePricePeriod(request.getPricePeriod()));
        } else if (isCreate) {
            if (listing.getListingPurpose() == PropertyListing.ListingPurpose.SALE) {
                listing.setPricePeriod(PropertyListing.PricePeriod.TOTAL);
            } else if (listing.getStayType() == PropertyListing.StayType.SHORT_TERM) {
                listing.setPricePeriod(PropertyListing.PricePeriod.NIGHT);
            } else {
                listing.setPricePeriod(PropertyListing.PricePeriod.MONTH);
            }
        }

        if (request.getSalePrice() != null) {
            listing.setSalePrice(request.getSalePrice());
        } else if (listing.getListingPurpose() == PropertyListing.ListingPurpose.SALE
                && request.getRentAmount() != null) {
            listing.setSalePrice(request.getRentAmount());
        }

        if (request.getMinStayDays() != null) {
            listing.setMinStayDays(request.getMinStayDays());
        }
        if (request.getMaxStayDays() != null) {
            listing.setMaxStayDays(request.getMaxStayDays());
        }

        if (request.getFurnishingStatus() != null) {
            listing.setFurnishingStatus(parseFurnishingStatus(request.getFurnishingStatus()));
            syncLegacyFurnishingFields(listing);
        } else if (request.getIsUnfurnished() != null) {
            listing.setIsUnfurnished(request.getIsUnfurnished());
            if (Boolean.TRUE.equals(request.getIsUnfurnished())) {
                listing.setFurnishingStatus(PropertyListing.FurnishingStatus.UNFURNISHED);
            } else if (listing.getFurnishingStatus() == PropertyListing.FurnishingStatus.UNFURNISHED) {
                listing.setFurnishingStatus(PropertyListing.FurnishingStatus.SEMI_FURNISHED);
            }
            syncLegacyFurnishingFields(listing);
        }

        if (request.getIsSharedAccommodation() != null) {
            listing.setIsSharedAccommodation(request.getIsSharedAccommodation());
        } else if (isCreate && listing.getPropertyType() == PropertyListing.PropertyType.SHARED_ROOM) {
            listing.setIsSharedAccommodation(true);
            listing.setRoommatesWanted(true);
        }

        if (request.getRoommatesWanted() != null) {
            listing.setRoommatesWanted(request.getRoommatesWanted());
        }
        if (request.getAvailableRoommateSlots() != null) {
            listing.setAvailableRoommateSlots(request.getAvailableRoommateSlots());
        }
        if (request.getSharedSpaceNotes() != null) {
            listing.setSharedSpaceNotes(request.getSharedSpaceNotes());
        }
        if (request.getRoommateCompatibilityEnabled() != null) {
            listing.setRoommateCompatibilityEnabled(request.getRoommateCompatibilityEnabled());
        }
        if (request.getPreferredRoommateGender() != null) {
            listing.setPreferredRoommateGender(blankToNull(request.getPreferredRoommateGender()));
        }
    }

    private void syncLegacyFurnishingFields(PropertyListing listing) {
        if (listing.getFurnishingStatus() == null) {
            return;
        }
        switch (listing.getFurnishingStatus()) {
            case FURNISHED -> {
                listing.setIsUnfurnished(false);
                List<String> amenities = listing.getAmenities() == null ? new ArrayList<>() : new ArrayList<>(listing.getAmenities());
                if (!amenities.contains("Furnished")) {
                    amenities.add("Furnished");
                }
                listing.setAmenities(amenities);
            }
            case UNFURNISHED -> listing.setIsUnfurnished(true);
            case SEMI_FURNISHED -> listing.setIsUnfurnished(false);
        }
    }

    private PropertyListing.ListingPurpose parseListingPurpose(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PropertyListing.ListingPurpose.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid listing purpose: " + value);
        }
    }

    private PropertyListing.StayType parseStayType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PropertyListing.StayType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid stay type: " + value);
        }
    }

    private PropertyListing.PricePeriod parsePricePeriod(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PropertyListing.PricePeriod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid price period: " + value);
        }
    }

    private PropertyListing.FurnishingStatus parseFurnishingStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PropertyListing.FurnishingStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid furnishing status: " + value);
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

    private PropertyListing.RoomPreviewStatus parseRoomPreviewStatus(String status) {
        if (status == null || status.isBlank()) {
            return PropertyListing.RoomPreviewStatus.NOT_ENABLED;
        }
        try {
            return PropertyListing.RoomPreviewStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid room preview status: " + status);
        }
    }

    private void assertListingPhotoUrl(UUID listingId, String photoUrl) {
        boolean isListingPhoto = photoRepository.findByListingId(listingId).stream()
                .anyMatch(photo -> photoUrl.equals(photo.getPhotoUrl()));
        if (!isListingPhoto) {
            throw new BadRequestException("Room Preview photo must be one of this listing's uploaded photos");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean hasRoomDimensions(PropertyListing listing) {
        return listing.getRoomLengthMeters() != null && listing.getRoomWidthMeters() != null;
    }
    
    /** Public method for mapping listing to response (used by ElasticsearchSearchService). */
    public ListingResponse toListingResponse(PropertyListing listing, UUID userId) {
        return mapToResponse(listing, userId);
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

        LandlordVerification landlordVerification = landlordVerificationRepository
                .findByUserId(listing.getLandlord().getId())
                .orElse(null);
        String trustTier = computeTrustTier(listing, landlordVerification);
        Map<String, Boolean> qualitySignals = shouldExposeQualitySignals(listing, userId) ? qualitySignals(listing, photoDTOs) : null;
        
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
                .listingPurpose(listing.getListingPurpose() != null ? listing.getListingPurpose().name() : PropertyListing.ListingPurpose.RENT.name())
                .stayType(listing.getStayType() != null ? listing.getStayType().name() : PropertyListing.StayType.LONG_TERM.name())
                .pricePeriod(listing.getPricePeriod() != null ? listing.getPricePeriod().name() : PropertyListing.PricePeriod.MONTH.name())
                .salePrice(listing.getSalePrice())
                .minStayDays(listing.getMinStayDays())
                .maxStayDays(listing.getMaxStayDays())
                .furnishingStatus(listing.getFurnishingStatus() != null ? listing.getFurnishingStatus().name() : null)
                .isSharedAccommodation(Boolean.TRUE.equals(listing.getIsSharedAccommodation()))
                .roommatesWanted(Boolean.TRUE.equals(listing.getRoommatesWanted()))
                .availableRoommateSlots(listing.getAvailableRoommateSlots())
                .sharedSpaceNotes(listing.getSharedSpaceNotes())
                .roommateCompatibilityEnabled(Boolean.TRUE.equals(listing.getRoommateCompatibilityEnabled()))
                .preferredRoommateGender(listing.getPreferredRoommateGender())
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
                .isUnfurnished(Boolean.TRUE.equals(listing.getIsUnfurnished()))
                .roomPreviewEnabled(Boolean.TRUE.equals(listing.getRoomPreviewEnabled()))
                .roomLengthMeters(listing.getRoomLengthMeters())
                .roomWidthMeters(listing.getRoomWidthMeters())
                .roomHeightMeters(listing.getRoomHeightMeters())
                .roomPreviewPhotoUrl(listing.getRoomPreviewPhotoUrl())
                .roomPreviewStatus(listing.getRoomPreviewStatus() != null ? listing.getRoomPreviewStatus().name() : null)
                .availableFrom(listing.getAvailableFrom())
                .availableTo(listing.getAvailableTo())
                .status(listing.getStatus().name())
                .verified(listing.getVerified())
                .trustTier(trustTier)
                .trustSignals(trustSignals(listing, landlordVerification))
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
                .qualityScore(qualitySignals != null ? qualityScore(qualitySignals) : null)
                .qualitySignals(qualitySignals)
                .videoTourUrl(listing.getVideoTourUrl())
                .videoTourEmbedCode(listing.getVideoTourEmbedCode())
                .virtualTourProvider(listing.getVirtualTourProvider())
                .videoTourThumbnail(listing.getVideoTourThumbnail())
                .videoTourDuration(listing.getVideoTourDuration())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }

    private static boolean shouldExposeQualitySignals(PropertyListing listing, UUID userId) {
        try {
            String role = SecurityUtils.getCurrentUserRole();
            if ("ADMIN".equalsIgnoreCase(role)) {
                return true;
            }
            return "LANDLORD".equalsIgnoreCase(role)
                    && userId != null
                    && listing.getLandlord() != null
                    && userId.equals(listing.getLandlord().getId());
        } catch (Exception ignored) {
            return false;
        }
    }

    static Map<String, Boolean> qualitySignals(PropertyListing listing, List<PhotoDTO> photos) {
        Map<String, Boolean> signals = new java.util.LinkedHashMap<>();
        signals.put("hasPhotos", photos != null && photos.size() >= 3);
        signals.put("hasTitle", hasText(listing.getTitle()) && listing.getTitle().trim().length() >= 12);
        signals.put("hasDescription", hasText(listing.getDescription()) && listing.getDescription().trim().length() >= 80);
        signals.put("hasLocation", hasText(listing.getCity()) && hasText(listing.getNeighborhood()));
        signals.put("hasPrice", listing.getRentAmount() != null && listing.getRentAmount() > 0);
        signals.put("hasAmenities", listing.getAmenities() != null && listing.getAmenities().size() >= 3);
        signals.put("isReviewed", Boolean.TRUE.equals(listing.getVerified()));
        signals.put("hasAvailability", listing.getAvailableFrom() != null);
        signals.put("hasVideoOrTour", hasText(listing.getVideoTourUrl()) || hasText(listing.getVideoTourEmbedCode()));
        return signals;
    }

    static int qualityScore(Map<String, Boolean> signals) {
        if (signals == null || signals.isEmpty()) {
            return 0;
        }
        long passed = signals.values().stream().filter(Boolean.TRUE::equals).count();
        return (int) Math.round(passed * 100.0 / signals.size());
    }

    static Map<String, Boolean> trustSignals(PropertyListing listing, LandlordVerification landlordVerification) {
        Map<String, Boolean> signals = new java.util.LinkedHashMap<>();
        boolean listingReviewed = Boolean.TRUE.equals(listing.getVerified()) && listing.getStatus() == PropertyListing.Status.ACTIVE;
        boolean identityReviewed = landlordVerification != null
                && landlordVerification.getIdentityStatus() == LandlordVerification.VerificationStatus.VERIFIED;
        boolean propertyReviewed = landlordVerification != null
                && landlordVerification.getPropertyStatus() == LandlordVerification.VerificationStatus.VERIFIED;
        boolean landlordTrusted = landlordVerification != null
                && (Boolean.TRUE.equals(landlordVerification.getIsTrustedLandlord())
                || (landlordVerification.getTrustScore() != null && landlordVerification.getTrustScore() >= 70));

        signals.put("phoneVerified", listing.getLandlord() != null && Boolean.TRUE.equals(listing.getLandlord().getPhoneVerified()));
        signals.put("identityReviewed", identityReviewed);
        signals.put("landlordVerified", landlordTrusted || identityReviewed);
        signals.put("propertyReviewed", propertyReviewed);
        signals.put("listingApproved", listingReviewed);
        signals.put("responsiveLandlord", listing.getLandlord() != null && listing.getUpdatedAt() != null);
        return signals;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Tenant-facing trust tier: listing review (verified) plus landlord verification strength.
     */
    private static String computeTrustTier(PropertyListing listing, LandlordVerification lv) {
        boolean listingReviewed = Boolean.TRUE.equals(listing.getVerified());
        if (!listingReviewed) {
            return "STANDARD";
        }
        boolean trustedFlag = lv != null && Boolean.TRUE.equals(lv.getIsTrustedLandlord());
        int score = lv != null && lv.getTrustScore() != null ? lv.getTrustScore() : 0;
        boolean identityOk = lv != null && lv.getIdentityStatus() == LandlordVerification.VerificationStatus.VERIFIED;

        if (trustedFlag || score >= 70) {
            return "HIGH";
        }
        if (identityOk) {
            return "VERIFIED";
        }
        return "REVIEWED";
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
        enqueueSearchOutbox(saved, ListingSearchOutbox.EVENT_UPDATED);
        return mapToResponse(saved, landlordId);
    }

    // Mark listing as available by sending it through admin review.
    public ListingResponse markAsAvailable(UUID listingId, UUID landlordId) {
        PropertyListing listing = findListingByIdAndLandlord(listingId, landlordId);
        if (listing.getStatus() == PropertyListing.Status.ACTIVE) {
            log.info("Listing {} is already ACTIVE; mark available is a no-op", listingId);
        } else {
            listing.setStatus(PropertyListing.Status.PENDING);
            listing.setVerified(false);
            listing.setVerifiedAt(null);
            listing.setVerifiedBy(null);
            log.info("Listing {} set to PENDING for admin approval", listingId);
        }
        PropertyListing saved = listingRepository.save(listing);
        enqueueSearchOutbox(saved, ListingSearchOutbox.EVENT_UPDATED);
        return mapToResponse(saved, landlordId);
    }

    // Set video tour
    public ListingResponse setVideoTour(UUID listingId, UUID landlordId, String videoUrl) {
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        
        if (!listing.getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("Only the listing owner can set the video tour");
        }
        
        listing.setVideoTourUrl(videoUrl);
        listing = listingRepository.save(listing);
        enqueueSearchOutbox(listing, ListingSearchOutbox.EVENT_UPDATED);
        return mapToResponse(listing, null);
    }

    @Transactional(readOnly = true)
    public List<ArMarkerResponse> getArMarkers(UUID listingId) {
        return listingArMarkerRepository.findByListingIdOrderByCreatedAtAsc(listingId).stream()
                .map(this::mapArMarker)
                .collect(Collectors.toList());
    }

    public List<ArMarkerResponse> replaceArMarkers(UUID listingId, UUID landlordId, ArMarkersUpdateRequest request) {
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (!listing.getLandlord().getId().equals(landlordId)) {
            throw new BadRequestException("Only the listing owner can update AR markers");
        }

        listingArMarkerRepository.deleteByListingId(listingId);
        List<ListingArMarker> markers = request.getMarkers().stream()
                .map(item -> ListingArMarker.builder()
                        .listing(listing)
                        .x(item.getX())
                        .y(item.getY())
                        .z(item.getZ())
                        .label(item.getLabel() != null ? item.getLabel().trim() : null)
                        .color(item.getColor() != null ? item.getColor().trim() : null)
                        .build())
                .collect(Collectors.toList());
        return listingArMarkerRepository.saveAll(markers).stream()
                .map(this::mapArMarker)
                .collect(Collectors.toList());
    }

    private ArMarkerResponse mapArMarker(ListingArMarker marker) {
        return ArMarkerResponse.builder()
                .id(marker.getId())
                .x(marker.getX())
                .y(marker.getY())
                .z(marker.getZ())
                .label(marker.getLabel())
                .color(marker.getColor())
                .createdAt(marker.getCreatedAt())
                .build();
    }
    
    // Track view
    public void trackView(UUID listingId, UUID userId) {
        PropertyListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listing.setViewsCount(listing.getViewsCount() + 1);
        listingRepository.save(listing);
        if (userId != null) {
            securityAuditService.logAction("listing.view", userId, listingId, "LISTING");
        }
    }

    // Get statistics
    public Map<String, Object> getLandlordStatistics(UUID landlordId) {
        List<PropertyListing> listings = listingRepository.findByLandlordId(landlordId);
        
        // Count total favorites across all landlord's listings
        long totalFavorites = listings.stream()
                .mapToLong(l -> favoriteRepository.countByListingId(l.getId()))
                .sum();
        
        // Count applications
        long totalApplications = applicationRepository.countByLandlordId(landlordId);
        long pendingApplications = applicationRepository.countByLandlordIdAndStatus(
                landlordId, org.rooms.roombay.entity.RoomApplication.Status.PENDING);
        
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalListings", listings.size());
        stats.put("activeListings", listings.stream().filter(l -> l.getStatus() == PropertyListing.Status.ACTIVE).count());
        stats.put("rentedListings", listings.stream().filter(l -> l.getStatus() == PropertyListing.Status.RENTED).count());
        stats.put("totalViews", listings.stream().mapToInt(PropertyListing::getViewsCount).sum());
        stats.put("totalFavorites", totalFavorites);
        stats.put("totalApplications", totalApplications);
        stats.put("pendingApplications", pendingApplications);
        return stats;
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

@Transactional(readOnly = true)
public List<ListingResponse> getAllListings(String status) {
    log.info("Fetching all listings with status filter: {}", status);
    
    List<PropertyListing> listings;
    if (status != null && !status.isEmpty()) {
        try {
            PropertyListing.Status statusEnum = PropertyListing.Status.valueOf(status.toUpperCase());
            listings = listingRepository.findByStatus(statusEnum);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status filter: {}", status);
            listings = listingRepository.findAll();
        }
    } else {
        listings = listingRepository.findAll();
    }
    
    return listings.stream()
            .map(listing -> mapToResponse(listing, null))
            .collect(Collectors.toList());
}

    /**
     * Tenant-facing trust tier for a listing (same rules as {@link #mapToResponse}).
     */
    @Transactional(readOnly = true)
    public String trustTierForListing(PropertyListing listing) {
        LandlordVerification lv = landlordVerificationRepository
                .findByUserId(listing.getLandlord().getId())
                .orElse(null);
        return computeTrustTier(listing, lv);
    }

    /**
     * Rule-based similar listings for listing detail ("More like this") or landlord comps.
     *
     * @param purpose {@code discover} (default) for tenant discovery; {@code comps} excludes same landlord and weights rent/location for pricing context
     */
    @Transactional(readOnly = true)
    public List<ListingRecommendationResponse> findSimilarListings(
            UUID seedListingId, UUID viewerUserId, String purpose, int limit) {
        PropertyListing seed = listingRepository.findById(seedListingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + seedListingId));

        boolean comps = purpose != null && "comps".equalsIgnoreCase(purpose.trim());
        int cap = Math.min(Math.max(limit, 1), 36);

        List<PropertyListing> pool = listingRepository.findActiveVerifiedListings().stream()
                .filter(l -> !l.getId().equals(seedListingId))
                .filter(l -> !comps || !l.getLandlord().getId().equals(seed.getLandlord().getId()))
                .collect(Collectors.toList());

        List<ScoredSimilar> scored = new ArrayList<>();
        for (PropertyListing c : pool) {
            Set<String> codeSet = new LinkedHashSet<>();
            int score = 0;

            if (sameNorm(seed.getCity(), c.getCity())) {
                codeSet.add("SAME_CITY");
                score += comps ? 22 : 16;
            }
            if (sameNorm(seed.getNeighborhood(), c.getNeighborhood())) {
                codeSet.add("SAME_NEIGHBORHOOD");
                score += comps ? 38 : 32;
            }
            if (seed.getPropertyType() != null && seed.getPropertyType().equals(c.getPropertyType())) {
                codeSet.add("SAME_PROPERTY_TYPE");
                score += 18;
            }

            if (seed.getBedrooms() != null && c.getBedrooms() != null) {
                int diff = Math.abs(seed.getBedrooms() - c.getBedrooms());
                if (diff == 0) {
                    codeSet.add("SAME_BEDROOMS");
                    score += comps ? 24 : 14;
                } else if (diff == 1) {
                    codeSet.add("SIMILAR_BEDROOMS");
                    score += 8;
                }
            }

            if (seed.getRentAmount() != null && c.getRentAmount() != null && seed.getRentAmount() > 0) {
                double pct = Math.abs(c.getRentAmount() - seed.getRentAmount()) * 100.0 / seed.getRentAmount();
                if (pct <= 10) {
                    codeSet.add("SIMILAR_RENT");
                    score += comps ? 42 : 22;
                } else if (pct <= 20) {
                    codeSet.add("CLOSE_RENT");
                    score += comps ? 28 : 12;
                }
            }

            if (comps) {
                codeSet.add("MARKET_COMPARABLE");
            }

            if (score == 0) {
                score = 4;
                codeSet.add("ACTIVE_NEAR_MARKET");
            }

            List<String> codes = codeSet.stream().limit(5).collect(Collectors.toList());
            scored.add(new ScoredSimilar(c, score, codes));
        }

        scored.sort(Comparator.comparingInt(ScoredSimilar::score).reversed());
        return scored.stream()
                .limit(cap)
                .map(s -> mapToListingRecommendation(s.listing(), viewerUserId, s.score(), s.codes(), comps))
                .collect(Collectors.toList());
    }

    private record ScoredSimilar(PropertyListing listing, int score, List<String> codes) {}

    private static boolean sameNorm(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private ListingRecommendationResponse mapToListingRecommendation(
            PropertyListing listing,
            UUID viewerUserId,
            int matchScore,
            List<String> reasonCodes,
            boolean comps) {
        ListingPhoto primary = photoRepository.findByListingIdOrderByDisplayOrderAsc(listing.getId()).stream()
                .filter(ListingPhoto::getIsPrimary)
                .findFirst()
                .orElse(null);
        if (primary == null) {
            List<ListingPhoto> photos = photoRepository.findByListingIdOrderByDisplayOrderAsc(listing.getId());
            if (!photos.isEmpty()) {
                primary = photos.get(0);
            }
        }

        boolean isViewed = viewerUserId != null && listingViewRepository.hasUserViewedListing(viewerUserId, listing.getId());
        boolean isFavorited = viewerUserId != null && favoriteRepository.existsByUserIdAndListingId(viewerUserId, listing.getId());
        Double averageRating = reviewRepository.getAverageRatingForListing(listing.getId());
        long reviewCount = reviewRepository.countReviewsForListing(listing.getId());
        String trust = trustTierForListing(listing);

        List<String> legacyReasons = new ArrayList<>(reasonCodes.stream()
                .map(code -> humanizeSimilarCode(code, comps))
                .collect(Collectors.toList()));

        return ListingRecommendationResponse.builder()
                .listingId(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .rentAmount(listing.getRentAmount())
                .city(listing.getCity())
                .neighborhood(listing.getNeighborhood())
                .propertyType(listing.getPropertyType() != null ? listing.getPropertyType().name() : null)
                .primaryPhotoUrl(primary != null ? primary.getPhotoUrl() : null)
                .bedrooms(listing.getBedrooms())
                .bathrooms(listing.getBathrooms())
                .amenities(listing.getAmenities())
                .status(listing.getStatus() != null ? listing.getStatus().name() : null)
                .availableFrom(listing.getAvailableFrom())
                .availableTo(listing.getAvailableTo())
                .isAvailable(listing.getStatus() != null && listing.getStatus() == PropertyListing.Status.ACTIVE)
                .matchScore(Math.min(100, matchScore))
                .preferenceScore(0)
                .behaviorScore(0)
                .reasons(legacyReasons)
                .reasonCodes(reasonCodes)
                .isViewed(isViewed)
                .isFavorited(isFavorited)
                .viewsCount(listing.getViewsCount())
                .verified(listing.getVerified())
                .featured(listing.getFeatured())
                .trustTier(trust)
                .averageRating(averageRating)
                .reviewCount((int) reviewCount)
                .build();
    }

    private static String humanizeSimilarCode(String code, boolean comps) {
        return switch (code) {
            case "SAME_NEIGHBORHOOD" -> comps ? "Same neighborhood (market)" : "Same neighborhood";
            case "SAME_CITY" -> "Same city";
            case "SAME_PROPERTY_TYPE" -> "Same property type";
            case "SAME_BEDROOMS" -> "Same bedroom count";
            case "SIMILAR_BEDROOMS" -> "Similar size";
            case "SIMILAR_RENT" -> comps ? "Similar asking rent" : "Similar rent";
            case "CLOSE_RENT" -> "Close rent range";
            case "MARKET_COMPARABLE" -> "Market comparable";
            case "ACTIVE_NEAR_MARKET" -> "Active listing near you";
            default -> code.replace('_', ' ').toLowerCase();
        };
    }
}
