package org.rooms.roombay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.entity.Match;
import org.rooms.roombay.entity.RoommatePreferences;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.dto.request.MatchActionRequest;
import org.rooms.roombay.repository.MatchRepository;
import org.rooms.roombay.repository.RoommatePreferencesRepository;
import org.rooms.roombay.repository.UserRepository;
import org.rooms.roombay.repository.ProfileRepository;

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

    @Mock
    private SecurityAuditService securityAuditService;
    
    @InjectMocks
    private MatchingService matchingService;
    
    private UUID userId1;
    private UUID userId2;
    private User user1;
    private User user2;
    private RoommatePreferences prefs1;
    
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

    @Test
    void testAcceptOrRejectMatch_SecondAcceptTriggersMutualNotification() {
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .user1(user1)
                .user2(user2)
                .status(Match.Status.PENDING)
                .user1Action(Match.UserAction.ACCEPT)
                .build();
        MatchActionRequest request = new MatchActionRequest();
        request.setAction("ACCEPT");

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());

        var response = matchingService.acceptOrRejectMatch(userId2, match.getId(), request);

        assertEquals("MUTUAL", response.getStatus());
        verify(notificationService, times(1)).notifyMutualMatch(eq(userId1), eq(match.getId()), anyString());
        verify(notificationService, times(1)).notifyMutualMatch(eq(userId2), eq(match.getId()), anyString());
    }
}

