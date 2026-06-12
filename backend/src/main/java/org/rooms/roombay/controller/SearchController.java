package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.ListingResponse;
import org.rooms.roombay.search.ElasticsearchSearchService;
import org.rooms.roombay.service.AnalyticsEventService;
import org.rooms.roombay.service.ListingService;
import org.rooms.roombay.util.InputSanitizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Elasticsearch-backed search endpoint. When Elasticsearch is enabled, uses fuzzy matching,
 * typo tolerance, boosting (featured, verified), and language-aware relevance.
 * Falls back to DB search when Elasticsearch is disabled.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search", description = "Elasticsearch-backed search with fuzzy matching and boosting")
public class SearchController {

    private final Optional<ElasticsearchSearchService> elasticsearchSearchService;
    private final ListingService listingService;
    private final InputSanitizer inputSanitizer;
    private final AnalyticsEventService analyticsEventService;

    @GetMapping
    @Operation(summary = "Search listings (Elasticsearch)", description = "Search with fuzzy matching, typo tolerance, boosting (featured, verified), and language-aware relevance. Uses Elasticsearch when enabled.")
    public ResponseEntity<Page<ListingResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Integer bathrooms,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLon,
            @RequestParam(required = false) String availableFrom,
            @RequestParam(required = false) String listingPurpose,
            @RequestParam(required = false) String stayType,
            @RequestParam(required = false) String furnishingStatus,
            @RequestParam(required = false) Boolean sharedAccommodation,
            @RequestParam(required = false, defaultValue = "en") String lang,
            @RequestParam(required = false, defaultValue = "relevance") String mode,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        try {
            query = inputSanitizer.sanitizeString(query);
            city = inputSanitizer.sanitizeCity(city);
            neighborhood = inputSanitizer.sanitizeString(neighborhood);
            propertyType = inputSanitizer.sanitizePropertyType(propertyType);
            minPrice = inputSanitizer.sanitizeInteger(minPrice, 0, 10000000);
            maxPrice = inputSanitizer.sanitizeInteger(maxPrice, 0, 10000000);
            bedrooms = inputSanitizer.sanitizeInteger(bedrooms, 0, 20);
            bathrooms = inputSanitizer.sanitizeInteger(bathrooms, 0, 20);
            amenities = inputSanitizer.sanitizeList(amenities);
            maxDistance = inputSanitizer.sanitizeDistance(maxDistance);
            userLat = inputSanitizer.sanitizeLatitude(userLat);
            userLon = inputSanitizer.sanitizeLongitude(userLon);
            availableFrom = inputSanitizer.sanitizeDate(availableFrom);
            listingPurpose = inputSanitizer.sanitizeListingPurpose(listingPurpose);
            stayType = inputSanitizer.sanitizeStayType(stayType);
            furnishingStatus = inputSanitizer.sanitizeFurnishingStatus(furnishingStatus);
        } catch (SecurityException e) {
            log.warn("Security violation in search: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        analyticsEventService.emit("search_performed", userId, null, null,
                java.util.Map.of("source", "search", "query", query != null ? query : "", "city", city != null ? city : ""));

        if (elasticsearchSearchService.isPresent()) {
            log.info("Searching via Elasticsearch - query: {}, city: {}, lang: {}", query, city, lang);
            Page<ListingResponse> results = elasticsearchSearchService.get().search(
                    query, city, neighborhood, propertyType, minPrice, maxPrice,
                    bedrooms, bathrooms, amenities, maxDistance, userLat, userLon,
                    lang, mode, userId, pageable);
            // Fallback to DB when ES returns nothing (e.g. index still populating)
            if (results.isEmpty() && results.getTotalElements() == 0) {
                log.info("Elasticsearch returned 0 results - falling back to DB search");
                Pageable fallbackPageable = pageable;
                if ("recent".equalsIgnoreCase(mode)) {
                    fallbackPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
                } else if ("trending".equalsIgnoreCase(mode)) {
                    fallbackPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("viewsCount").descending().and(Sort.by("createdAt").descending()));
                }
                results = listingService.searchListingsAdvanced(
                        query, city, neighborhood, propertyType, minPrice, maxPrice,
                        bedrooms, bathrooms, amenities, maxDistance, userLat, userLon,
                        availableFrom, listingPurpose, stayType, furnishingStatus, sharedAccommodation,
                        userId, fallbackPageable);
            }
            return ResponseEntity.ok(results);
        }

        // Elasticsearch disabled - use DB search
        log.info("Searching via DB (Elasticsearch disabled) - query: {}, city: {}", query, city);
        Page<ListingResponse> results = listingService.searchListingsAdvanced(
                query, city, neighborhood, propertyType, minPrice, maxPrice,
                bedrooms, bathrooms, amenities, maxDistance, userLat, userLon,
                availableFrom, listingPurpose, stayType, furnishingStatus, sharedAccommodation,
                userId, pageable);
        return ResponseEntity.ok(results);
    }
}
