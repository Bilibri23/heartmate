package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    
    private final UserRepository userRepository;
    private final PropertyListingRepository listingRepository;
    private final MatchRepository matchRepository;
    private final RoomApplicationRepository applicationRepository;
    private final MessageRepository messageRepository;
    private final ReportRepository reportRepository;
    
    /**
     * Get overview analytics - platform summary
     */
    public Map<String, Object> getOverviewAnalytics() {
        log.info("Fetching overview analytics");
        
        Map<String, Object> analytics = new HashMap<>();
        
        // Total counts
        analytics.put("totalUsers", userRepository.count());
        analytics.put("totalListings", listingRepository.count());
        analytics.put("totalMatches", matchRepository.count());
        analytics.put("totalApplications", applicationRepository.count());
        analytics.put("totalMessages", messageRepository.count());
        analytics.put("totalReports", reportRepository != null ? reportRepository.count() : 0);
        
        // User breakdown
        Map<String, Long> usersByRole = new HashMap<>();
        usersByRole.put("students", userRepository.countByRole(User.UserRole.STUDENT));
        usersByRole.put("landlords", userRepository.countByRole(User.UserRole.LANDLORD));
        usersByRole.put("admins", userRepository.countByRole(User.UserRole.ADMIN));
        analytics.put("usersByRole", usersByRole);
        
        // User status breakdown
        Map<String, Long> usersByStatus = new HashMap<>();
        usersByStatus.put("active", userRepository.countByAccountStatus(User.AccountStatus.ACTIVE));
        usersByStatus.put("pending", userRepository.countByAccountStatus(User.AccountStatus.PENDING));
        usersByStatus.put("suspended", userRepository.countByAccountStatus(User.AccountStatus.SUSPENDED));
        usersByStatus.put("deactivated", userRepository.countByAccountStatus(User.AccountStatus.DEACTIVATED));
        analytics.put("usersByStatus", usersByStatus);
        
        // Verification stats
        analytics.put("verifiedUsers", userRepository.countByEmailVerifiedAndPhoneVerified(true, true));
        analytics.put("unverifiedUsers", userRepository.count() - userRepository.countByEmailVerifiedAndPhoneVerified(true, true));
        
        // Activity stats (last 7 days)
        LocalDateTime last7Days = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        analytics.put("newUsersLast7Days", userRepository.countByCreatedAtAfter(last7Days));
        analytics.put("newListingsLast7Days", listingRepository.countByCreatedAtAfter(last7Days));
        
        // Activity stats (last 30 days)
        LocalDateTime last30Days = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        analytics.put("newUsersLast30Days", userRepository.countByCreatedAtAfter(last30Days));
        analytics.put("newListingsLast30Days", listingRepository.countByCreatedAtAfter(last30Days));
        
        return analytics;
    }
    
    /**
     * Get user growth analytics
     */
    public Map<String, Object> getUserGrowthAnalytics() {
        log.info("Fetching user growth analytics");
        
        Map<String, Object> analytics = new HashMap<>();
        
        // Current totals
        analytics.put("totalUsers", userRepository.count());
        analytics.put("totalStudents", userRepository.countByRole(User.UserRole.STUDENT));
        analytics.put("totalLandlords", userRepository.countByRole(User.UserRole.LANDLORD));
        
        // Growth metrics
        LocalDateTime last7Days = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        LocalDateTime last30Days = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        LocalDateTime last90Days = LocalDateTime.now().minus(90, ChronoUnit.DAYS);
        
        analytics.put("newUsersLast7Days", userRepository.countByCreatedAtAfter(last7Days));
        analytics.put("newUsersLast30Days", userRepository.countByCreatedAtAfter(last30Days));
        analytics.put("newUsersLast90Days", userRepository.countByCreatedAtAfter(last90Days));
        
        // Verification rate
        long totalUsers = userRepository.count();
        long verifiedUsers = userRepository.countByEmailVerifiedAndPhoneVerified(true, true);
        double verificationRate = totalUsers > 0 ? (verifiedUsers * 100.0 / totalUsers) : 0;
        analytics.put("verificationRate", Math.round(verificationRate * 100.0) / 100.0);
        
        // Profile completion rate
        long completedProfiles = userRepository.countByProfileCompleted(true);
        double completionRate = totalUsers > 0 ? (completedProfiles * 100.0 / totalUsers) : 0;
        analytics.put("profileCompletionRate", Math.round(completionRate * 100.0) / 100.0);
        
        return analytics;
    }
    
    /**
     * Get listing analytics
     */
    public Map<String, Object> getListingAnalytics() {
        log.info("Fetching listing analytics");
        
        Map<String, Object> analytics = new HashMap<>();
        
        // Total counts
        analytics.put("totalListings", listingRepository.count());
        analytics.put("activeListings", listingRepository.countByDeletedFalse());
        analytics.put("deletedListings", listingRepository.countByDeletedTrue());
        
        // Growth metrics
        LocalDateTime last7Days = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        LocalDateTime last30Days = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        
        analytics.put("newListingsLast7Days", listingRepository.countByCreatedAtAfter(last7Days));
        analytics.put("newListingsLast30Days", listingRepository.countByCreatedAtAfter(last30Days));
        
        // Popular locations (top 10 cities)
        List<Object[]> topCities = listingRepository.findTopCitiesByListingCount();
        Map<String, Long> popularCities = new HashMap<>();
        for (Object[] row : topCities) {
            String city = (String) row[0];
            Long count = (Long) row[1];
            popularCities.put(city, count);
        }
        analytics.put("topCities", popularCities);
        
        return analytics;
    }
    
    /**
     * Get engagement analytics
     */
    public Map<String, Object> getEngagementAnalytics() {
        log.info("Fetching engagement analytics");
        
        Map<String, Object> analytics = new HashMap<>();
        
        // Match stats
        analytics.put("totalMatches", matchRepository.count());
        analytics.put("mutualMatches", matchRepository.countMutualMatches());
        analytics.put("pendingMatches", matchRepository.countPendingMatches());
        
        // Application stats
        analytics.put("totalApplications", applicationRepository.count());
        analytics.put("pendingApplications", applicationRepository.countPendingApplications());
        analytics.put("approvedApplications", applicationRepository.countApprovedApplications());
        
        // Messaging stats
        analytics.put("totalMessages", messageRepository.count());
        
        // Calculate activity rates
        long totalUsers = userRepository.count();
        long usersWithMatches = matchRepository.countUsersWithMatches();
        long usersWithApplications = applicationRepository.countUsersWithApplications();
        
        double matchEngagementRate = totalUsers > 0 ? (usersWithMatches * 100.0 / totalUsers) : 0;
        double applicationEngagementRate = totalUsers > 0 ? (usersWithApplications * 100.0 / totalUsers) : 0;
        
        analytics.put("matchEngagementRate", Math.round(matchEngagementRate * 100.0) / 100.0);
        analytics.put("applicationEngagementRate", Math.round(applicationEngagementRate * 100.0) / 100.0);
        
        return analytics;
    }
    
    /**
     * Get conversion analytics
     */
    public Map<String, Object> getConversionAnalytics() {
        log.info("Fetching conversion analytics");
        
        Map<String, Object> analytics = new HashMap<>();
        
        // Student to application conversion
        long totalStudents = userRepository.countByRole(User.UserRole.STUDENT);
        long studentsWithApplications = applicationRepository.countUsersWithApplications();
        double studentToApplicationRate = totalStudents > 0 ? (studentsWithApplications * 100.0 / totalStudents) : 0;
        
        // Landlord to listing conversion
        long totalLandlords = userRepository.countByRole(User.UserRole.LANDLORD);
        long landlordsWithListings = listingRepository.countDistinctLandlords();
        double landlordToListingRate = totalLandlords > 0 ? (landlordsWithListings * 100.0 / totalLandlords) : 0;
        
        // Match to message conversion
        long totalMatches = matchRepository.countMutualMatches();
        long matchesWithMessages = messageRepository.countConversations();
        double matchToMessageRate = totalMatches > 0 ? (matchesWithMessages * 100.0 / totalMatches) : 0;
        
        analytics.put("studentToApplicationRate", Math.round(studentToApplicationRate * 100.0) / 100.0);
        analytics.put("landlordToListingRate", Math.round(landlordToListingRate * 100.0) / 100.0);
        analytics.put("matchToMessageRate", Math.round(matchToMessageRate * 100.0) / 100.0);
        
        return analytics;
    }
    
    /**
     * Get complete dashboard analytics
     */
    public Map<String, Object> getDashboardAnalytics() {
        log.info("Fetching complete dashboard analytics");
        
        Map<String, Object> dashboard = new HashMap<>();
        
        dashboard.put("overview", getOverviewAnalytics());
        dashboard.put("userGrowth", getUserGrowthAnalytics());
        dashboard.put("listings", getListingAnalytics());
        dashboard.put("engagement", getEngagementAnalytics());
        dashboard.put("conversions", getConversionAnalytics());
        
        return dashboard;
    }
}
