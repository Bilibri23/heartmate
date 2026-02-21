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
    private final ListingPreferencesRepository listingPreferencesRepository;
    private final ListingViewRepository viewRepository;
    private final ListingFavoriteRepository favoriteRepository;
    private final ProfileRepository profileRepository;
    
    // Scoring weights
    private static final int MAX_RECOMMENDATIONS = 20;
    private static final double PREFERENCE_WEIGHT = 0.50;  // 50% from preferences
    private static final double BEHAVIOR_WEIGHT = 0.30;    // 30% from behavior
    private static final double SIMILAR_USER_WEIGHT = 0.15; // 15% from similar users
    private static final double DIVERSITY_WEIGHT = 0.05;    // 5% for diversity
    
    /**
     * Get personalized listing recommendations for a student
     * Combines: preferences + past viewing behavior + similar user behavior
     */
    public List<ScoredListing> getRecommendedListings(UUID studentId) {
        log.info("Generating recommendations for student: {}", studentId);
        
        // Get student's preferences
        Optional<ListingPreferences> prefsOpt = listingPreferencesRepository.findByUserId(studentId);
        if (prefsOpt.isEmpty()) {
            log.warn("No preferences found for student {}, returning popular listings", studentId);
            return getPopularListings(MAX_RECOMMENDATIONS);
        }
        
        ListingPreferences prefs = prefsOpt.get();
        
        // Get all active listings (verified status boosts score but doesn't exclude)
        List<PropertyListing> allListings = listingRepository.findByStatus(
            PropertyListing.Status.ACTIVE
        );
        
        // Get behavioral data
        List<ListingView> recentViews = viewRepository.findRecentByUserId(
            studentId, LocalDateTime.now().minusDays(30)
        );
        List<UUID> favoritedListingIds = favoriteRepository.findByUserId(studentId).stream()
            .map(fav -> fav.getListing().getId())
            .collect(Collectors.toList());
        
        // Find similar users (collaborative filtering)
        List<UUID> similarUserIds = findSimilarUsers(studentId, recentViews, favoritedListingIds);
        
        // Score each listing (LinkedIn-style: always show relevant content)
        List<ScoredListing> scoredListings = new ArrayList<>();
        Set<String> usedNeighborhoods = new HashSet<>(); // For diversity
        Set<PropertyListing.PropertyType> usedTypes = new HashSet<>(); // For diversity
        
        for (PropertyListing listing : allListings) {
            // Skip if already favorited (they already saved it)
            if (favoritedListingIds.contains(listing.getId())) {
                continue;
            }
            
            // Calculate preference-based score
            int preferenceScore = calculatePreferenceScore(prefs, listing);
            
            // Calculate behavioral boost
            int behaviorBoost = calculateBehaviorBoost(listing, recentViews, favoritedListingIds);
            
            // Calculate similar user boost (collaborative filtering)
            int similarUserBoost = calculateSimilarUserBoost(listing, similarUserIds);
            
            // Add engagement signals (like LinkedIn's engagement-based ranking)
            int engagementBoost = calculateEngagementBoost(listing);
            
            // Diversity boost (encourage variety)
            int diversityBoost = calculateDiversityBoost(listing, usedNeighborhoods, usedTypes);
            
            // Combined score with all factors
            // Ensure we always have a base score from engagement (even if preferences are 0)
            int baseScore = (int) (engagementBoost * 0.2); // At least engagement score
            
            // Verified listings get a bonus
            int verifiedBoost = Boolean.TRUE.equals(listing.getVerified()) ? 15 : 0;
            
            int totalScore = (int) (
                (preferenceScore * PREFERENCE_WEIGHT) + 
                (behaviorBoost * BEHAVIOR_WEIGHT) +
                (similarUserBoost * SIMILAR_USER_WEIGHT) +
                (diversityBoost * DIVERSITY_WEIGHT) +
                baseScore +
                verifiedBoost
            );
            
            // Ensure minimum score of 30 (so users see meaningful match percentages)
            // This ensures listings always have a visible match score
            totalScore = Math.max(totalScore, 30);
            
            // Track for diversity
            if (listing.getNeighborhood() != null) {
                usedNeighborhoods.add(listing.getNeighborhood());
            }
            if (listing.getPropertyType() != null) {
                usedTypes.add(listing.getPropertyType());
            }
            
            // Always include listings, let sorting handle relevance (LinkedIn approach)
            scoredListings.add(ScoredListing.builder()
                .listing(listing)
                .totalScore(totalScore)
                .preferenceScore(preferenceScore)
                .behaviorScore(behaviorBoost)
                .reasons(generateReasons(prefs, listing, preferenceScore, behaviorBoost, similarUserBoost))
                .build());
        }
        
        // Sort by score and return top N with diversity
        scoredListings.sort((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()));
        
        // Apply diversity filter: ensure we don't show too many from same area/type
        List<ScoredListing> topRecommendations = applyDiversityFilter(
            scoredListings, MAX_RECOMMENDATIONS
        );
        
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
    private int calculatePreferenceScore(ListingPreferences prefs, PropertyListing listing) {
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
        if (prefs.getPropertyTypes() != null && !prefs.getPropertyTypes().isEmpty() && listing.getPropertyType() != null) {
            boolean matchesType = prefs.getPropertyTypes().stream()
                .anyMatch(type -> type.equalsIgnoreCase(listing.getPropertyType().name()));
            score += matchesType ? 100 : 40;
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
     * Find similar users based on viewing and favoriting behavior (collaborative filtering)
     */
    private List<UUID> findSimilarUsers(UUID studentId, 
                                        List<ListingView> recentViews,
                                        List<UUID> favoritedListingIds) {
        if (recentViews.isEmpty() && favoritedListingIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Build user's interest profile
        Set<UUID> userInterestedListings = new HashSet<>();
        recentViews.forEach(view -> userInterestedListings.add(view.getListing().getId()));
        userInterestedListings.addAll(favoritedListingIds);
        
        if (userInterestedListings.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Find users who viewed the same listings (collaborative filtering)
        List<UUID> candidateUserIds = viewRepository.findUsersWhoViewedListings(
            new ArrayList<>(userInterestedListings), studentId
        );
        
        // Also check favorites - find users who favorited same listings
        for (UUID listingId : favoritedListingIds) {
            // Get all favorites for this listing
            List<ListingFavorite> allFavorites = favoriteRepository.findAll().stream()
                .filter(f -> f.getListing().getId().equals(listingId) 
                          && !f.getUser().getId().equals(studentId))
                .collect(Collectors.toList());
            
            for (ListingFavorite fav : allFavorites) {
                UUID otherUserId = fav.getUser().getId();
                if (!candidateUserIds.contains(otherUserId)) {
                    candidateUserIds.add(otherUserId);
                }
            }
        }
        
        // Score users by how many listings they have in common
        Map<UUID, Integer> userSimilarityScores = new HashMap<>();
        for (UUID candidateId : candidateUserIds) {
            List<ListingView> candidateViews = viewRepository.findRecentByUserId(
                candidateId, LocalDateTime.now().minusDays(60)
            );
            List<UUID> candidateFavorites = favoriteRepository.findByUserId(candidateId).stream()
                .map(fav -> fav.getListing().getId())
                .collect(Collectors.toList());
            
            int commonListings = 0;
            for (UUID listingId : userInterestedListings) {
                boolean candidateViewed = candidateViews.stream()
                    .anyMatch(v -> v.getListing().getId().equals(listingId));
                boolean candidateFavorited = candidateFavorites.contains(listingId);
                
                if (candidateViewed || candidateFavorited) {
                    commonListings++;
                }
            }
            
            if (commonListings > 0) {
                userSimilarityScores.put(candidateId, commonListings);
            }
        }
        
        // Get top 10 similar users
        return userSimilarityScores.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate boost based on similar users' preferences (collaborative filtering)
     */
    private int calculateSimilarUserBoost(PropertyListing listing, List<UUID> similarUserIds) {
        if (similarUserIds.isEmpty()) {
            return 0;
        }
        
        int boost = 0;
        int matches = 0;
        
        // Check if similar users viewed/favorited this listing
        for (UUID similarUserId : similarUserIds) {
            boolean hasViewed = viewRepository.hasUserViewedListing(similarUserId, listing.getId());
            boolean hasFavorited = favoriteRepository.existsByUserIdAndListingId(similarUserId, listing.getId());
            
            if (hasViewed || hasFavorited) {
                matches++;
            }
        }
        
        // Boost based on how many similar users liked it
        if (matches > 0) {
            double similarityRatio = (double) matches / similarUserIds.size();
            boost = (int) (similarityRatio * 100); // Up to 100 points
        }
        
        return Math.min(100, boost);
    }
    
    /**
     * Calculate diversity boost to encourage variety in recommendations
     */
    private int calculateDiversityBoost(PropertyListing listing,
                                       Set<String> usedNeighborhoods,
                                       Set<PropertyListing.PropertyType> usedTypes) {
        int boost = 0;
        
        // Boost if this is a new neighborhood we haven't shown yet
        if (listing.getNeighborhood() != null && !usedNeighborhoods.contains(listing.getNeighborhood())) {
            boost += 15;
        }
        
        // Boost if this is a new property type we haven't shown yet
        if (listing.getPropertyType() != null && !usedTypes.contains(listing.getPropertyType())) {
            boost += 10;
        }
        
        return boost;
    }
    
    /**
     * Apply diversity filter to ensure variety in recommendations
     */
    private List<ScoredListing> applyDiversityFilter(List<ScoredListing> scoredListings, int limit) {
        List<ScoredListing> diverseList = new ArrayList<>();
        Set<String> usedNeighborhoods = new HashSet<>();
        Set<PropertyListing.PropertyType> usedTypes = new HashSet<>();
        Map<String, Integer> neighborhoodCount = new HashMap<>();
        Map<PropertyListing.PropertyType, Integer> typeCount = new HashMap<>();
        
        // First pass: Add top scoring listings with diversity consideration
        for (ScoredListing scored : scoredListings) {
            PropertyListing listing = scored.getListing();
            String neighborhood = listing.getNeighborhood();
            PropertyListing.PropertyType type = listing.getPropertyType();
            
            // Allow max 3 listings per neighborhood, 4 per type
            int neighborhoodUsage = neighborhoodCount.getOrDefault(neighborhood, 0);
            int typeUsage = typeCount.getOrDefault(type, 0);
            
            if (neighborhoodUsage < 3 && typeUsage < 4) {
                diverseList.add(scored);
                if (neighborhood != null) {
                    neighborhoodCount.put(neighborhood, neighborhoodUsage + 1);
                }
                if (type != null) {
                    typeCount.put(type, typeUsage + 1);
                }
                
                if (diverseList.size() >= limit) {
                    break;
                }
            }
        }
        
        // If we don't have enough, fill with remaining top scores
        if (diverseList.size() < limit) {
            for (ScoredListing scored : scoredListings) {
                if (!diverseList.contains(scored)) {
                    diverseList.add(scored);
                    if (diverseList.size() >= limit) {
                        break;
                    }
                }
            }
        }
        
        return diverseList;
    }
    
    /**
     * Generate human-readable reasons for recommendation
     */
    private List<String> generateReasons(ListingPreferences prefs, 
                                         PropertyListing listing,
                                         int preferenceScore,
                                         int behaviorScore,
                                         int similarUserBoost) {
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

        // Property type match
        if (prefs.getPropertyTypes() != null && !prefs.getPropertyTypes().isEmpty() && listing.getPropertyType() != null) {
            boolean matchesType = prefs.getPropertyTypes().stream()
                .anyMatch(type -> type.equalsIgnoreCase(listing.getPropertyType().name()));
            if (matchesType) {
                reasons.add("Matches your preferred property type");
            }
        }
        
        // Behavioral signals
        if (behaviorScore > 60) {
            reasons.add("Similar to listings you've viewed");
        }
        
        // Similar user signals (collaborative filtering)
        if (similarUserBoost > 50) {
            reasons.add("Loved by students like you");
        } else if (similarUserBoost > 20) {
            reasons.add("Popular with similar students");
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
