package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.entity.*;
import org.rooms.roombuddy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent recommendation engine for listings and roommates
 * Combines preference-based matching with behavioral signals
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecommendationService {
    
    private final PropertyListingRepository listingRepository;
    private final RoommatePreferencesRepository preferencesRepository;
    private final ListingViewRepository viewRepository;
    private final ListingFavoriteRepository favoriteRepository;
    private final ProfileRepository profileRepository;
    
    // Scoring weights
    private static final int MAX_RECOMMENDATIONS = 20;
    private static final double PREFERENCE_WEIGHT = 0.60;  // 60% from preferences
    private static final double BEHAVIOR_WEIGHT = 0.40;    // 40% from behavior
    
    /**
     * Get personalized listing recommendations for a student
     * Combines: preferences + past viewing behavior + similar user behavior
     */
    @org.springframework.cache.annotation.Cacheable(value = "recommendations", key = "#studentId")
    public List<ScoredListing> getRecommendedListings(UUID studentId) {
        log.info("Generating recommendations for student: {}", studentId);
        
        // Get student's preferences
        Optional<RoommatePreferences> prefsOpt = preferencesRepository.findByUserId(studentId);
        if (prefsOpt.isEmpty()) {
            log.warn("No preferences found for student {}, returning popular listings", studentId);
            return getPopularListings(MAX_RECOMMENDATIONS);
        }
        
        RoommatePreferences prefs = prefsOpt.get();
        
        // Get all active verified listings
        List<PropertyListing> allListings = listingRepository.findByStatusAndVerified(
            PropertyListing.Status.ACTIVE, true
        );
        
        // Get behavioral data
        List<ListingView> recentViews = viewRepository.findRecentByUserId(
            studentId, LocalDateTime.now().minusDays(30)
        );
        List<UUID> favoritedListingIds = favoriteRepository.findByUserId(studentId).stream()
            .map(fav -> fav.getListing().getId())
            .collect(Collectors.toList());
        
        // Score each listing (LinkedIn-style: always show relevant content)
        List<ScoredListing> scoredListings = new ArrayList<>();
        for (PropertyListing listing : allListings) {
            // Skip if already favorited (they already saved it)
            if (favoritedListingIds.contains(listing.getId())) {
                continue;
            }
            
            // Calculate preference-based score
            int preferenceScore = calculatePreferenceScore(prefs, listing);
            
            // Calculate behavioral boost
            int behaviorBoost = calculateBehaviorBoost(listing, recentViews, favoritedListingIds);
            
            // Add engagement signals (like LinkedIn's engagement-based ranking)
            int engagementBoost = calculateEngagementBoost(listing);
            
            // Combined score with engagement factor
            int totalScore = (int) ((preferenceScore * PREFERENCE_WEIGHT) + 
                                   (behaviorBoost * BEHAVIOR_WEIGHT * 0.7) +
                                   (engagementBoost * 0.3));
            
            // Always include listings, let sorting handle relevance (LinkedIn approach)
            scoredListings.add(ScoredListing.builder()
                .listing(listing)
                .totalScore(Math.max(totalScore, 10)) // Minimum score of 10
                .preferenceScore(preferenceScore)
                .behaviorScore(behaviorBoost)
                .reasons(generateReasons(prefs, listing, preferenceScore, behaviorBoost))
                .build());
        }
        
        // Sort by score and return top N
        scoredListings.sort((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()));
        
        List<ScoredListing> topRecommendations = scoredListings.stream()
            .limit(MAX_RECOMMENDATIONS)
            .collect(Collectors.toList());
        
        // If no recommendations, fall back to popular listings
        if (topRecommendations.isEmpty()) {
            log.info("No scored recommendations, falling back to popular listings for {}", studentId);
            return getPopularListings(MAX_RECOMMENDATIONS);
        }
        
        log.info("Generated {} recommendations for student {}", topRecommendations.size(), studentId);
        return topRecommendations;
    }
    
    /**
     * Calculate score based on user preferences
     */
    private int calculatePreferenceScore(RoommatePreferences prefs, PropertyListing listing) {
        int score = 0;
        int factors = 0;
        
        // Budget match (most important - 40%)
        if (prefs.getMinBudget() != null && prefs.getMaxBudget() != null) {
            int rent = listing.getRentAmount();
            if (rent >= prefs.getMinBudget() && rent <= prefs.getMaxBudget()) {
                score += 100; // Perfect match
            } else if (rent < prefs.getMinBudget()) {
                // Under budget is OK
                score += 80;
            } else {
                // Over budget - penalize based on how much over
                int overBy = rent - prefs.getMaxBudget();
                int budgetRange = prefs.getMaxBudget() - prefs.getMinBudget();
                if (budgetRange > 0) {
                    int penalty = (overBy * 100) / budgetRange;
                    score += Math.max(0, 100 - penalty);
                }
            }
            factors++;
        }
        
        // Location match (30%)
        if (prefs.getPreferredLocations() != null && !prefs.getPreferredLocations().isEmpty()) {
            boolean matchesLocation = prefs.getPreferredLocations().stream()
                .anyMatch(loc -> loc.equalsIgnoreCase(listing.getCity()) || 
                               loc.equalsIgnoreCase(listing.getNeighborhood()));
            
            score += matchesLocation ? 100 : 20; // Big bonus for location match
            factors++;
        }
        
        // Distance to university (20%)
        if (prefs.getMaxDistanceFromCampus() != null && listing.getDistanceToUniversity() != null) {
            if (listing.getDistanceToUniversity().compareTo(prefs.getMaxDistanceFromCampus()) <= 0) {
                score += 100; // Within range
            } else {
                // Calculate penalty
                BigDecimal excess = listing.getDistanceToUniversity().subtract(prefs.getMaxDistanceFromCampus());
                int penalty = excess.multiply(BigDecimal.valueOf(20)).intValue();
                score += Math.max(0, 100 - penalty);
            }
            factors++;
        }
        
        // Property type preference (10%)
        // TODO: Add property type preference to RoommatePreferences
        // For now, give slight bonus to studios and private rooms for students
        if (listing.getPropertyType() == PropertyListing.PropertyType.STUDIO ||
            listing.getPropertyType() == PropertyListing.PropertyType.PRIVATE_ROOM) {
            score += 70;
            factors++;
        }
        
        return factors > 0 ? score / factors : 50;
    }
    
    /**
     * Calculate engagement boost based on listing popularity (LinkedIn-style signals)
     */
    private int calculateEngagementBoost(PropertyListing listing) {
        int boost = 0;
        
        // Views indicate interest from community
        int views = listing.getViewsCount() != null ? listing.getViewsCount() : 0;
        if (views > 100) boost += 25;
        else if (views > 50) boost += 20;
        else if (views > 20) boost += 15;
        else if (views > 5) boost += 10;
        
        // Favorites indicate high quality
        int favorites = listing.getFavoritesCount() != null ? listing.getFavoritesCount() : 0;
        if (favorites > 20) boost += 25;
        else if (favorites > 10) boost += 20;
        else if (favorites > 5) boost += 15;
        else if (favorites > 0) boost += 10;
        
        // Good ratings boost visibility
        Double rating = listing.getAverageRating();
        if (rating != null && rating > 0) {
            if (rating >= 4.5) boost += 25;
            else if (rating >= 4.0) boost += 20;
            else if (rating >= 3.5) boost += 15;
            else if (rating >= 3.0) boost += 10;
        }
        
        // Featured listings get priority
        if (Boolean.TRUE.equals(listing.getFeatured())) {
            boost += 20;
        }
        
        // Fresh content boost (like LinkedIn's recency signal)
        if (listing.getCreatedAt() != null) {
            long daysOld = java.time.temporal.ChronoUnit.DAYS.between(
                listing.getCreatedAt().toLocalDate(), 
                java.time.LocalDate.now()
            );
            if (daysOld <= 1) boost += 20;      // Posted today/yesterday
            else if (daysOld <= 3) boost += 15; // Last 3 days
            else if (daysOld <= 7) boost += 10; // Last week
        }
        
        return Math.min(100, boost);
    }
    
    /**
     * Calculate boost based on user behavior
     */
    private int calculateBehaviorBoost(PropertyListing listing, 
                                       List<ListingView> recentViews,
                                       List<UUID> favoritedListingIds) {
        int boost = 0;
        
        // Build profile of what user has viewed/liked
        Map<String, Integer> neighborhoodInterest = new HashMap<>();
        Map<PropertyListing.PropertyType, Integer> typeInterest = new HashMap<>();
        int minViewedPrice = Integer.MAX_VALUE;
        int maxViewedPrice = Integer.MIN_VALUE;
        
        for (ListingView view : recentViews) {
            PropertyListing viewedListing = view.getListing();
            
            // Track neighborhood interest
            String neighborhood = viewedListing.getNeighborhood();
            if (neighborhood != null) {
                neighborhoodInterest.put(neighborhood, 
                    neighborhoodInterest.getOrDefault(neighborhood, 0) + 1);
            }
            
            // Track property type interest
            PropertyListing.PropertyType type = viewedListing.getPropertyType();
            if (type != null) {
                typeInterest.put(type, typeInterest.getOrDefault(type, 0) + 1);
            }
            
            // Track price range
            int price = viewedListing.getRentAmount();
            minViewedPrice = Math.min(minViewedPrice, price);
            maxViewedPrice = Math.max(maxViewedPrice, price);
        }
        
        // Apply boosts
        
        // Same neighborhood as frequently viewed (+30)
        String listingNeighborhood = listing.getNeighborhood();
        if (listingNeighborhood != null && neighborhoodInterest.containsKey(listingNeighborhood)) {
            int views = neighborhoodInterest.get(listingNeighborhood);
            boost += Math.min(30, views * 10); // Up to +30
        }
        
        // Same property type as frequently viewed (+25)
        PropertyListing.PropertyType listingType = listing.getPropertyType();
        if (listingType != null && typeInterest.containsKey(listingType)) {
            int views = typeInterest.get(listingType);
            boost += Math.min(25, views * 8); // Up to +25
        }
        
        // Similar price range to viewed listings (+20)
        int listingPrice = listing.getRentAmount();
        if (minViewedPrice != Integer.MAX_VALUE) {
            int priceRange = maxViewedPrice - minViewedPrice;
            int midPrice = (maxViewedPrice + minViewedPrice) / 2;
            int priceDiff = Math.abs(listingPrice - midPrice);
            
            if (priceRange > 0) {
                double similarity = 1.0 - ((double) priceDiff / priceRange);
                boost += (int) (Math.max(0, similarity) * 20);
            }
        }
        
        // Same city as favorited listings (+15)
        // TODO: Implement when we have access to favorited listings details
        
        return Math.min(100, boost); // Cap at 100
    }
    
    /**
     * Generate human-readable reasons for recommendation
     */
    private List<String> generateReasons(RoommatePreferences prefs, 
                                         PropertyListing listing,
                                         int preferenceScore,
                                         int behaviorScore) {
        List<String> reasons = new ArrayList<>();
        
        // Budget match
        if (prefs.getMinBudget() != null && prefs.getMaxBudget() != null) {
            int rent = listing.getRentAmount();
            if (rent >= prefs.getMinBudget() && rent <= prefs.getMaxBudget()) {
                reasons.add("Within your budget (" + rent + " XAF/month)");
            }
        }
        
        // Location match
        if (prefs.getPreferredLocations() != null && !prefs.getPreferredLocations().isEmpty()) {
            boolean matchesLocation = prefs.getPreferredLocations().stream()
                .anyMatch(loc -> loc.equalsIgnoreCase(listing.getCity()) || 
                               loc.equalsIgnoreCase(listing.getNeighborhood()));
            
            if (matchesLocation) {
                reasons.add("In your preferred area (" + listing.getNeighborhood() + ")");
            }
        }
        
        // Distance to university
        if (listing.getDistanceToUniversity() != null) {
            reasons.add("Only " + listing.getDistanceToUniversity() + " km from university");
        }
        
        // Behavioral signals
        if (behaviorScore > 60) {
            reasons.add("Similar to listings you've viewed");
        }
        
        // Amenities
        if (listing.getAmenities() != null && !listing.getAmenities().isEmpty()) {
            List<String> keyAmenities = listing.getAmenities().stream()
                .filter(a -> a.contains("WiFi") || a.contains("Water") || a.contains("Electricity"))
                .limit(2)
                .collect(Collectors.toList());
            
            if (!keyAmenities.isEmpty()) {
                reasons.add("Has " + String.join(", ", keyAmenities));
            }
        }
        
        // Featured/Verified
        if (listing.getFeatured()) {
            reasons.add("Featured listing");
        }
        if (listing.getVerified()) {
            reasons.add("Verified by admin");
        }
        
        return reasons.stream().limit(4).collect(Collectors.toList());
    }
    
    /**
     * Fallback: Get popular listings when no preference data
     */
    private List<ScoredListing> getPopularListings(int limit) {
        List<PropertyListing> popular = listingRepository.findTopByViewsCount(limit);
        
        return popular.stream()
            .map(listing -> ScoredListing.builder()
                .listing(listing)
                .totalScore(80)
                .preferenceScore(0)
                .behaviorScore(0)
                .reasons(List.of("Popular listing", listing.getViewsCount() + " views"))
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Track when a user views a listing (for behavioral data)
     */
    @Transactional
    public void trackListingView(UUID userId, UUID listingId, String source) {
        try {
            User user = new User();
            user.setId(userId);
            
            PropertyListing listing = new PropertyListing();
            listing.setId(listingId);
            
            ListingView view = ListingView.builder()
                .user(user)
                .listing(listing)
                .source(source)
                .build();
            
            viewRepository.save(view);
            log.debug("Tracked view: user={}, listing={}, source={}", userId, listingId, source);
        } catch (Exception e) {
            // Don't fail the main request if tracking fails
            log.error("Failed to track listing view", e);
        }
    }
    
    /**
     * DTO for scored listing recommendation
     */
    @lombok.Data
    @lombok.Builder
    public static class ScoredListing {
        private PropertyListing listing;
        private Integer totalScore;
        private Integer preferenceScore;
        private Integer behaviorScore;
        private List<String> reasons;
    }
}
