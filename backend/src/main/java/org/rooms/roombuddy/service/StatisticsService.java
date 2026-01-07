package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.response.StatisticsResponse;
import org.rooms.roombuddy.entity.Match;
import org.rooms.roombuddy.entity.PropertyListing;
import org.rooms.roombuddy.entity.StudentVerification;
import org.rooms.roombuddy.entity.User;

import java.util.List;
import org.rooms.roombuddy.repository.MatchRepository;
import org.rooms.roombuddy.repository.PropertyListingRepository;
import org.rooms.roombuddy.repository.StudentVerificationRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatisticsService {
    
    private final UserRepository userRepository;
    private final StudentVerificationRepository verificationRepository;
    private final PropertyListingRepository listingRepository;
    private final MatchRepository matchRepository;
    
    public StatisticsResponse getStatistics() {
        log.info("Fetching platform statistics");
        
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(User.UserRole.STUDENT);
        long totalLandlords = userRepository.countByRole(User.UserRole.LANDLORD);
        
        long verifiedStudents = verificationRepository.countByStatus(StudentVerification.Status.VERIFIED);
        long pendingVerifications = verificationRepository.countByStatus(StudentVerification.Status.PENDING);
        
        List<PropertyListing> activeListingsList = listingRepository.findByStatusAndVerified(
                PropertyListing.Status.ACTIVE, true);
        long activeListings = activeListingsList.size();
        
        List<PropertyListing> pendingListingsList = listingRepository.findByStatus(
                PropertyListing.Status.PENDING);
        long pendingListings = pendingListingsList.size();
        
        long totalMatches = matchRepository.count();
        long acceptedMatches = matchRepository.countByStatus(Match.Status.ACCEPTED);
        
        return StatisticsResponse.builder()
                .totalUsers(totalUsers)
                .totalStudents(totalStudents)
                .totalLandlords(totalLandlords)
                .verifiedStudents(verifiedStudents)
                .activeListings(activeListings)
                .pendingListings(pendingListings)
                .totalMatches(totalMatches)
                .acceptedMatches(acceptedMatches)
                .pendingVerifications(pendingVerifications)
                .pendingListingApprovals(pendingListings)
                .build();
    }
}

