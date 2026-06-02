package org.rooms.roombay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnlineStatusServiceTest {
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @InjectMocks
    private OnlineStatusService onlineStatusService;
    
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }
    
    @Test
    void testMarkUserOnline() {
        // When
        onlineStatusService.markUserOnline(userId);
        
        // Then
        assertTrue(onlineStatusService.isUserOnline(userId));
        verify(messagingTemplate).convertAndSend(eq("/topic/online-status"), any(Object.class));
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/online-status"), any(Object.class));
    }
    
    @Test
    void testMarkUserOffline() {
        // Given
        onlineStatusService.markUserOnline(userId);
        assertTrue(onlineStatusService.isUserOnline(userId));
        
        // When
        onlineStatusService.markUserOffline(userId);
        
        // Then
        assertFalse(onlineStatusService.isUserOnline(userId));
    }
    
    @Test
    void testUpdateLastSeen() {
        // When
        onlineStatusService.updateLastSeen(userId);
        
        // Then
        assertTrue(onlineStatusService.isUserOnline(userId));
        assertNotNull(onlineStatusService.getLastSeen(userId));
    }
    
    @Test
    void testIsUserOnline_WhenNeverOnline_ReturnsFalse() {
        // When/Then
        assertFalse(onlineStatusService.isUserOnline(userId));
    }
}
