package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.MatchActionRequest;
import org.rooms.roombuddy.dto.response.MatchResponse;
import org.rooms.roombuddy.entity.Match;
import org.rooms.roombuddy.entity.Profile;
import org.rooms.roombuddy.entity.RoommatePreferences;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.MatchRepository;
import org.rooms.roombuddy.repository.ProfileRepository;
import org.rooms.roombuddy.repository.RoommatePreferencesRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MatchingService {
    
    private final MatchRepository matchRepository;
    private final RoommatePreferencesRepository preferencesRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    
    // Compatibility weights (as per requirements)
    private static final double BUDGET_WEIGHT = 0.30;
    private static final double LIFESTYLE_WEIGHT = 0.25;
    private static final double SCHEDULE_WEIGHT = 0.20;
    private static final double LOCATION_WEIGHT = 0.15;
    private static final double HABITS_WEIGHT = 0.10;
    private static final int MIN_COMPATIBILITY_THRESHOLD = 60;
    private static final int MAX_MATCHES = 20;
    
    public List<MatchResponse> findMatches(UUID userId) {
        log.info("Finding matches for user: {}", userId);
        
        // Get user's preferences
        RoommatePreferences userPreferences = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Preferences not found for user: " + userId));
        
        // Check if user is looking for roommate
        if (!userPreferences.getLookingForRoommate()) {
            log.info("User {} is not looking for roommate", userId);
            return new ArrayList<>();
        }
        
        // Get all users looking for roommates (excluding current user)
        List<RoommatePreferences> allPreferences = preferencesRepository.findByLookingForRoommate(true);
        
        List<Match> matches = new ArrayList<>();
        
        for (RoommatePreferences otherPreferences : allPreferences) {
            UUID otherUserId = otherPreferences.getUser().getId();
            
            // Skip self
            if (otherUserId.equals(userId)) {
                continue;
            }
            
            // Check if match already exists
            if (matchRepository.existsMatchBetweenUsers(userId, otherUserId)) {
                continue;
            }
            
            // Check deal-breakers first
            if (hasDealBreakers(userPreferences, otherPreferences)) {
                continue;
            }
            
            // Calculate compatibility score
            CompatibilityScore score = calculateCompatibility(userPreferences, otherPreferences, userId, otherUserId);
            
            // Only include matches above threshold
            if (score.getOverallScore() >= MIN_COMPATIBILITY_THRESHOLD) {
                // Create match
                Match match = Match.builder()
                        .user1(userPreferences.getUser())
                        .user2(otherPreferences.getUser())
                        .compatibilityScore(score.getOverallScore())
                        .budgetScore(score.getBudgetScore())
                        .lifestyleScore(score.getLifestyleScore())
                        .scheduleScore(score.getScheduleScore())
                        .locationScore(score.getLocationScore())
                        .habitsScore(score.getHabitsScore())
                        .status(Match.Status.PENDING)
                        .algorithmVersion("1.0")
                        .build();
                
                matches.add(match);
            }
        }
        
        // Sort by compatibility score (descending) and limit to top 20
        matches.sort((a, b) -> Integer.compare(b.getCompatibilityScore(), a.getCompatibilityScore()));
        matches = matches.stream().limit(MAX_MATCHES).collect(Collectors.toList());
        
        // Save matches
        List<Match> savedMatches = matchRepository.saveAll(matches);
        log.info("Found {} matches for user: {}", savedMatches.size(), userId);
        
        // Send notifications for new matches
        for (Match match : savedMatches) {
            try {
                User user1 = match.getUser1();
                User user2 = match.getUser2();
                String user2Name = user2.getFirstName() + " " + user2.getLastName();
                String user1Name = user1.getFirstName() + " " + user1.getLastName();
                
                // Notify user1 about match with user2
                notificationService.notifyNewMatch(
                    user1.getId(),
                    match.getId(),
                    user2Name,
                    match.getCompatibilityScore()
                );
                
                // Notify user2 about match with user1
                notificationService.notifyNewMatch(
                    user2.getId(),
                    match.getId(),
                    user1Name,
                    match.getCompatibilityScore()
                );
            } catch (Exception e) {
                log.error("Failed to send match notification: {}", e.getMessage());
                // Don't fail the match creation if notification fails
            }
        }
        
        // Map to response
        return savedMatches.stream()
                .map(match -> mapToMatchResponse(match, userId))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<MatchResponse> getMatches(UUID userId, Match.Status status) {
        log.info("Getting matches for user: {} with status: {}", userId, status);
        
        List<Match> matches;
        if (status != null) {
            matches = matchRepository.findByUserIdAndStatus(userId, status);
        } else {
            matches = matchRepository.findByUserId(userId);
        }
        
        return matches.stream()
                .map(match -> mapToMatchResponse(match, userId))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<MatchResponse> getPendingMatches(UUID userId) {
        log.info("Getting pending matches for user: {}", userId);
        return getMatches(userId, Match.Status.PENDING);
    }
    
    /**
     * Get recommended matches for a user (top matches sorted by compatibility)
     */
    @org.springframework.cache.annotation.Cacheable(value = "matches", key = "'recommended:' + #userId")
    @Transactional(readOnly = true)
    public List<MatchResponse> getRecommendedMatches(UUID userId, int limit) {
        log.info("Getting recommended matches for user: {} (limit: {})", userId, limit);
        
        List<Match> matches = matchRepository.findByUserIdAndStatus(userId, Match.Status.PENDING);
        
        // Sort by compatibility score (descending) and limit
        matches.sort((a, b) -> Integer.compare(b.getCompatibilityScore(), a.getCompatibilityScore()));
        
        return matches.stream()
                .limit(limit)
                .map(match -> mapToMatchResponse(match, userId))
                .collect(Collectors.toList());
    }
    
    public MatchResponse acceptOrRejectMatch(UUID userId, UUID matchId, MatchActionRequest request) {
        log.info("User {} {} match: {}", userId, request.getAction(), matchId);
        
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));
        
        // Verify user is part of this match
        UUID user1Id = match.getUser1().getId();
        UUID user2Id = match.getUser2().getId();
        
        if (!userId.equals(user1Id) && !userId.equals(user2Id)) {
            throw new BadRequestException("User is not part of this match");
        }
        
        // Parse action
        Match.UserAction action;
        try {
            action = Match.UserAction.valueOf(request.getAction().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid action: " + request.getAction() + ". Must be ACCEPT or REJECT");
        }
        
        // Set user action
        if (userId.equals(user1Id)) {
            match.setUser1Action(action);
        } else {
            match.setUser2Action(action);
        }
        
        // Check if this creates a mutual match
        boolean wasMutualBefore = match.isMutualMatch();
        
        // Update match status
        if (match.isMutualMatch()) {
            match.setStatus(Match.Status.MUTUAL);
            
            // Send mutual match notification if it just became mutual
            if (!wasMutualBefore) {
                try {
                    User user1 = match.getUser1();
                    User user2 = match.getUser2();
                    String user2Name = user2.getFirstName() + " " + user2.getLastName();
                    String user1Name = user1.getFirstName() + " " + user1.getLastName();
                    
                    notificationService.notifyMutualMatch(user1.getId(), match.getId(), user2Name);
                    notificationService.notifyMutualMatch(user2.getId(), match.getId(), user1Name);
                } catch (Exception e) {
                    log.error("Failed to send mutual match notification: {}", e.getMessage());
                }
            }
            // Send notification emails
            emailService.sendMatchAcceptedEmail(match.getUser1().getEmail(), match.getUser2().getFirstName() + " " + match.getUser2().getLastName());
            emailService.sendMatchAcceptedEmail(match.getUser2().getEmail(), match.getUser1().getFirstName() + " " + match.getUser1().getLastName());
        } else if (match.isRejected()) {
            match.setStatus(Match.Status.REJECTED);
        }
        
        Match updated = matchRepository.save(match);
        log.info("Match {} updated with action: {}", matchId, action);
        
        return mapToMatchResponse(updated, userId);
    }
    
    private boolean hasDealBreakers(RoommatePreferences pref1, RoommatePreferences pref2) {
        // Check if pref1 has deal-breakers that pref2 violates
        if (pref1.getDealBreakers() != null && !pref1.getDealBreakers().trim().isEmpty()) {
            String dealBreakers = pref1.getDealBreakers().toLowerCase();
            User user2 = pref2.getUser();
            
            // Check for common deal-breakers
            if (dealBreakers.contains("smoking") && Boolean.TRUE.equals(pref2.getSmoking())) {
                return true;
            }
            if (dealBreakers.contains("drinking") && Boolean.TRUE.equals(pref2.getDrinking())) {
                return true;
            }
            if (dealBreakers.contains("pets") && Boolean.TRUE.equals(pref2.getPets())) {
                return true;
            }
            if (dealBreakers.contains("parties") && Boolean.TRUE.equals(pref2.getGuests())) {
                return true;
            }
        }
        
        // Check if pref2 has deal-breakers that pref1 violates
        if (pref2.getDealBreakers() != null && !pref2.getDealBreakers().trim().isEmpty()) {
            String dealBreakers = pref2.getDealBreakers().toLowerCase();
            
            if (dealBreakers.contains("smoking") && Boolean.TRUE.equals(pref1.getSmoking())) {
                return true;
            }
            if (dealBreakers.contains("drinking") && Boolean.TRUE.equals(pref1.getDrinking())) {
                return true;
            }
            if (dealBreakers.contains("pets") && Boolean.TRUE.equals(pref1.getPets())) {
                return true;
            }
            if (dealBreakers.contains("parties") && Boolean.TRUE.equals(pref1.getGuests())) {
                return true;
            }
        }
        
        return false;
    }
    
    private CompatibilityScore calculateCompatibility(
            RoommatePreferences pref1, RoommatePreferences pref2, UUID userId1, UUID userId2) {
        
        // Get user details for age/gender checks
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId1));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId2));
        
        // Calculate individual scores
        int budgetScore = calculateBudgetScore(pref1, pref2);
        int lifestyleScore = calculateLifestyleScore(pref1, pref2);
        int scheduleScore = calculateScheduleScore(pref1, pref2);
        int locationScore = calculateLocationScore(pref1, pref2);
        int habitsScore = calculateHabitsScore(pref1, pref2);
        
        // Apply weights and calculate overall score
        double overallScore = (budgetScore * BUDGET_WEIGHT) +
                (lifestyleScore * LIFESTYLE_WEIGHT) +
                (scheduleScore * SCHEDULE_WEIGHT) +
                (locationScore * LOCATION_WEIGHT) +
                (habitsScore * HABITS_WEIGHT);
        
        // Check gender preference
        if (!matchesGenderPreference(pref1, user2.getGender()) ||
            !matchesGenderPreference(pref2, user1.getGender())) {
            overallScore *= 0.5; // Reduce score by 50% if gender preference doesn't match
        }
        
        // Check age preference
        if (!matchesAgePreference(pref1, user2.getDateOfBirth()) ||
            !matchesAgePreference(pref2, user1.getDateOfBirth())) {
            overallScore *= 0.7; // Reduce score by 30% if age preference doesn't match
        }
        
        return CompatibilityScore.builder()
                .overallScore((int) Math.round(overallScore))
                .budgetScore(budgetScore)
                .lifestyleScore(lifestyleScore)
                .scheduleScore(scheduleScore)
                .locationScore(locationScore)
                .habitsScore(habitsScore)
                .build();
    }
    
    private int calculateBudgetScore(RoommatePreferences pref1, RoommatePreferences pref2) {
        if (pref1.getMinBudget() == null || pref1.getMaxBudget() == null ||
            pref2.getMinBudget() == null || pref2.getMaxBudget() == null) {
            return 50; // Neutral score if budget not set
        }
        
        // Check if budgets overlap
        int pref1Min = pref1.getMinBudget();
        int pref1Max = pref1.getMaxBudget();
        int pref2Min = pref2.getMinBudget();
        int pref2Max = pref2.getMaxBudget();
        
        if (pref1Max >= pref2Min && pref2Max >= pref1Min) {
            // Budgets overlap - calculate overlap percentage
            int overlapMin = Math.max(pref1Min, pref2Min);
            int overlapMax = Math.min(pref1Max, pref2Max);
            int overlap = overlapMax - overlapMin;
            int pref1Range = pref1Max - pref1Min;
            int pref2Range = pref2Max - pref2Min;
            int avgRange = (pref1Range + pref2Range) / 2;
            
            if (avgRange > 0) {
                return Math.min(100, 50 + (overlap * 100 / avgRange));
            }
        }
        
        // Budgets don't overlap - calculate distance
        int distance = Math.min(Math.abs(pref1Max - pref2Min), Math.abs(pref2Max - pref1Min));
        int avgBudget = (pref1Max + pref2Max) / 2;
        
        if (avgBudget > 0) {
            return Math.max(0, 50 - (distance * 50 / avgBudget));
        }
        
        return 0;
    }
    
    private int calculateLifestyleScore(RoommatePreferences pref1, RoommatePreferences pref2) {
        int score = 0;
        int factors = 0;
        
        // Cleanliness level (difference)
        if (pref1.getCleanlinessLevel() != null && pref2.getCleanlinessLevel() != null) {
            int diff = Math.abs(pref1.getCleanlinessLevel() - pref2.getCleanlinessLevel());
            score += Math.max(0, 100 - (diff * 25)); // Max 25 points per level difference
            factors++;
        }
        
        // Noise tolerance (difference)
        if (pref1.getNoiseTolerance() != null && pref2.getNoiseTolerance() != null) {
            int diff = Math.abs(pref1.getNoiseTolerance() - pref2.getNoiseTolerance());
            score += Math.max(0, 100 - (diff * 25));
            factors++;
        }
        
        // Social level (difference)
        if (pref1.getSocialLevel() != null && pref2.getSocialLevel() != null) {
            int diff = Math.abs(pref1.getSocialLevel() - pref2.getSocialLevel());
            score += Math.max(0, 100 - (diff * 25));
            factors++;
        }
        
        return factors > 0 ? score / factors : 50;
    }
    
    private int calculateScheduleScore(RoommatePreferences pref1, RoommatePreferences pref2) {
        int score = 0;
        int factors = 0;
        
        // Sleep schedule
        if (pref1.getSleepSchedule() != null && pref2.getSleepSchedule() != null) {
            if (pref1.getSleepSchedule() == pref2.getSleepSchedule()) {
                score += 100;
            } else if (pref1.getSleepSchedule() == RoommatePreferences.SleepSchedule.FLEXIBLE ||
                      pref2.getSleepSchedule() == RoommatePreferences.SleepSchedule.FLEXIBLE) {
                score += 75;
            } else {
                score += 25; // Different sleep schedules
            }
            factors++;
        }
        
        // Study time preference
        if (pref1.getStudyTimePreference() != null && pref2.getStudyTimePreference() != null) {
            if (pref1.getStudyTimePreference() == pref2.getStudyTimePreference()) {
                score += 100;
            } else if (pref1.getStudyTimePreference() == RoommatePreferences.StudyTimePreference.FLEXIBLE ||
                      pref2.getStudyTimePreference() == RoommatePreferences.StudyTimePreference.FLEXIBLE) {
                score += 75;
            } else {
                score += 50; // Different study times
            }
            factors++;
        }
        
        return factors > 0 ? score / factors : 50;
    }
    
    private int calculateLocationScore(RoommatePreferences pref1, RoommatePreferences pref2) {
        int score = 50; // Base score
        
        // Preferred locations overlap
        if (pref1.getPreferredLocations() != null && pref2.getPreferredLocations() != null &&
            !pref1.getPreferredLocations().isEmpty() && !pref2.getPreferredLocations().isEmpty()) {
            long commonLocations = pref1.getPreferredLocations().stream()
                    .filter(loc -> pref2.getPreferredLocations().contains(loc))
                    .count();
            
            if (commonLocations > 0) {
                score += 50; // Bonus for common locations
            }
        }
        
        // Distance from campus (if both have preferences)
        if (pref1.getMaxDistanceFromCampus() != null && pref2.getMaxDistanceFromCampus() != null) {
            BigDecimal avgDistance = pref1.getMaxDistanceFromCampus()
                    .add(pref2.getMaxDistanceFromCampus())
                    .divide(BigDecimal.valueOf(2));
            
            // Closer to campus is better (simplified scoring)
            if (avgDistance.compareTo(BigDecimal.valueOf(5)) <= 0) {
                score += 20;
            } else if (avgDistance.compareTo(BigDecimal.valueOf(10)) <= 0) {
                score += 10;
            }
        }
        
        return Math.min(100, score);
    }
    
    private int calculateHabitsScore(RoommatePreferences pref1, RoommatePreferences pref2) {
        int score = 0;
        int factors = 0;
        
        // Smoking
        if (pref1.getSmoking() != null && pref2.getSmoking() != null) {
            if (pref1.getSmoking().equals(pref2.getSmoking())) {
                score += 100;
            } else {
                score += 0; // Mismatch in smoking preference
            }
            factors++;
        }
        
        // Drinking
        if (pref1.getDrinking() != null && pref2.getDrinking() != null) {
            if (pref1.getDrinking().equals(pref2.getDrinking())) {
                score += 100;
            } else {
                score += 50; // Less critical than smoking
            }
            factors++;
        }
        
        // Pets
        if (pref1.getPets() != null && pref2.getPets() != null) {
            if (pref1.getPets().equals(pref2.getPets())) {
                score += 100;
            } else {
                score += 50;
            }
            factors++;
        }
        
        // Guests
        if (pref1.getGuests() != null && pref2.getGuests() != null) {
            if (pref1.getGuests().equals(pref2.getGuests())) {
                score += 100;
            } else {
                score += 75; // Guests are more flexible
            }
            factors++;
        }
        
        // Cooking
        if (pref1.getCooking() != null && pref2.getCooking() != null) {
            if (pref1.getCooking().equals(pref2.getCooking())) {
                score += 100;
            } else {
                score += 75;
            }
            factors++;
        }
        
        return factors > 0 ? score / factors : 50;
    }
    
    private boolean matchesGenderPreference(RoommatePreferences preferences, User.Gender userGender) {
        if (preferences.getPreferredGender() == null ||
            preferences.getPreferredGender() == RoommatePreferences.PreferredGender.ANY) {
            return true;
        }
        
        return (preferences.getPreferredGender() == RoommatePreferences.PreferredGender.MALE && userGender == User.Gender.MALE) ||
               (preferences.getPreferredGender() == RoommatePreferences.PreferredGender.FEMALE && userGender == User.Gender.FEMALE);
    }
    
    private boolean matchesAgePreference(RoommatePreferences preferences, LocalDate userDateOfBirth) {
        if (userDateOfBirth == null || preferences.getMinAge() == null || preferences.getMaxAge() == null) {
            return true; // No age preference or missing data
        }
        
        int userAge = Period.between(userDateOfBirth, LocalDate.now()).getYears();
        return userAge >= preferences.getMinAge() && userAge <= preferences.getMaxAge();
    }
    
    private MatchResponse mapToMatchResponse(Match match, UUID currentUserId) {
        UUID otherUserId = match.getUser1().getId().equals(currentUserId) 
                ? match.getUser2().getId() 
                : match.getUser1().getId();
        
        User otherUser = match.getUser1().getId().equals(currentUserId) 
                ? match.getUser2() 
                : match.getUser1();
        
        Profile otherProfile = profileRepository.findByUserId(otherUserId).orElse(null);
        
        Match.UserAction userAction = match.getUser1().getId().equals(currentUserId) 
                ? match.getUser1Action() 
                : match.getUser2Action();
        
        Match.UserAction otherUserAction = match.getUser1().getId().equals(currentUserId) 
                ? match.getUser2Action() 
                : match.getUser1Action();
        
        // Get WhatsApp number if mutual match
        String matchedUserWhatsapp = null;
        if (match.isMutualMatch() && otherProfile != null) {
            matchedUserWhatsapp = otherProfile.getWhatsappNumber();
        }
        
        // Generate match explanation
        String explanation = generateMatchExplanation(match);
        
        return MatchResponse.builder()
                .id(match.getId())
                .userId(currentUserId)
                .matchedUserId(otherUserId)
                .matchedUserFirstName(otherUser.getFirstName())
                .matchedUserLastName(otherUser.getLastName())
                .matchedUserEmail(otherUser.getEmail())
                .matchedUserProfilePhotoUrl(otherProfile != null ? otherProfile.getProfilePhotoUrl() : null)
                .matchedUserWhatsapp(matchedUserWhatsapp) // Only visible on mutual match
                .compatibilityScore(match.getCompatibilityScore())
                .budgetScore(match.getBudgetScore())
                .lifestyleScore(match.getLifestyleScore())
                .scheduleScore(match.getScheduleScore())
                .locationScore(match.getLocationScore())
                .habitsScore(match.getHabitsScore())
                .status(match.getStatus().name())
                .userAction(userAction != null ? userAction.name() : null)
                .matchedUserAction(otherUserAction != null ? otherUserAction.name() : null)
                .isMutualMatch(match.isMutualMatch())
                .explanation(explanation)
                .createdAt(match.getCreatedAt())
                .updatedAt(match.getUpdatedAt())
                .build();
    }
    
    /**
     * Generate human-readable explanation for why users matched
     */
    private String generateMatchExplanation(Match match) {
        List<String> reasons = new ArrayList<>();
        
        if (match.getBudgetScore() != null && match.getBudgetScore() >= 80) {
            reasons.add("similar budget");
        }
        if (match.getLifestyleScore() != null && match.getLifestyleScore() >= 80) {
            reasons.add("compatible lifestyle");
        }
        if (match.getScheduleScore() != null && match.getScheduleScore() >= 80) {
            reasons.add("similar schedules");
        }
        if (match.getLocationScore() != null && match.getLocationScore() >= 80) {
            reasons.add("preferred location");
        }
        if (match.getHabitsScore() != null && match.getHabitsScore() >= 80) {
            reasons.add("compatible habits");
        }
        
        if (reasons.isEmpty()) {
            return "You matched based on overall compatibility";
        }
        
        return "You matched because: " + String.join(", ", reasons);
    }
    
    // Inner class for compatibility score
    @lombok.Builder
    @lombok.Data
    private static class CompatibilityScore {
        private int overallScore;
        private int budgetScore;
        private int lifestyleScore;
        private int scheduleScore;
        private int locationScore;
        private int habitsScore;
    }
}

