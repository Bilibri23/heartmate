package org.rooms.roombuddy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombuddy.entity.Match;
import org.rooms.roombuddy.entity.RoommatePreferences;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.repository.MatchRepository;
import org.rooms.roombuddy.repository.RoommatePreferencesRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.rooms.roombuddy.repository.ProfileRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {
    
    @Mock
    private MatchRepository matchRepository;
    
    @Mock
    private RoommatePreferencesRepository preferencesRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ProfileRepository profileRepository;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private MatchingService matchingService;
    
    private UUID userId1;
    private UUID userId2;
    private User user1;
    private User user2;
    private RoommatePreferences prefs1;
    private RoommatePreferences prefs2;
    
    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        
        user1 = User.builder()
            .id(userId1)
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .build();
        
        user2 = User.builder()
            .id(userId2)
            .firstName("Jane")
            .lastName("Smith")
            .email("jane@example.com")
            .build();
        
        prefs1 = RoommatePreferences.builder()
            .user(user1)
            .lookingForRoommate(true)
            .minBudget(50000)
            .maxBudget(100000)
            .build();
        
        prefs2 = RoommatePreferences.builder()
            .user(user2)
            .lookingForRoommate(true)
            .minBudget(60000)
            .maxBudget(110000)
            .build();
    }
    
    @Test
    void testFindMatches_WhenUserNotLookingForRoommate_ReturnsEmptyList() {
        // Given
        prefs1.setLookingForRoommate(false);
        when(preferencesRepository.findByUserId(userId1)).thenReturn(Optional.of(prefs1));
        
        // When
        var matches = matchingService.findMatches(userId1);
        
        // Then
        assertTrue(matches.isEmpty());
        verify(matchRepository, never()).saveAll(any());
    }
    
    @Test
    void testGetMatches_WithStatusFilter() {
        // Given
        Match match = Match.builder()
            .id(UUID.randomUUID())
            .user1(user1)
            .user2(user2)
            .compatibilityScore(85)
            .status(Match.Status.PENDING)
            .build();
        
        when(matchRepository.findByUserIdAndStatus(userId1, Match.Status.PENDING))
            .thenReturn(java.util.List.of(match));
        when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
        
        // When
        var matches = matchingService.getMatches(userId1, Match.Status.PENDING);
        
        // Then
        assertEquals(1, matches.size());
        assertEquals(85, matches.get(0).getCompatibilityScore());
    }
}

