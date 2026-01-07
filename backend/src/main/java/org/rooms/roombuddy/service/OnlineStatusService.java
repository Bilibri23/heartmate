package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track user online status for messaging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineStatusService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    // Store user online status: userId -> lastSeen timestamp
    private final Map<UUID, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();
    
    // Timeout for considering user offline (5 minutes)
    private static final int OFFLINE_TIMEOUT_MINUTES = 5;
    
    /**
     * Mark user as online
     */
    public void markUserOnline(UUID userId) {
        onlineUsers.put(userId, LocalDateTime.now());
        log.debug("User {} marked as online", userId);
        
        // Broadcast online status to relevant users
        broadcastOnlineStatus(userId, true);
    }
    
    /**
     * Mark user as offline
     */
    public void markUserOffline(UUID userId) {
        onlineUsers.remove(userId);
        log.debug("User {} marked as offline", userId);
        
        // Broadcast offline status
        broadcastOnlineStatus(userId, false);
    }
    
    /**
     * Update user's last seen timestamp (heartbeat)
     */
    public void updateLastSeen(UUID userId) {
        onlineUsers.put(userId, LocalDateTime.now());
    }
    
    /**
     * Check if user is online
     */
    public boolean isUserOnline(UUID userId) {
        LocalDateTime lastSeen = onlineUsers.get(userId);
        if (lastSeen == null) {
            return false;
        }
        
        // Check if user is still within timeout period
        LocalDateTime timeout = lastSeen.plusMinutes(OFFLINE_TIMEOUT_MINUTES);
        boolean isOnline = LocalDateTime.now().isBefore(timeout);
        
        // Auto-remove if offline
        if (!isOnline) {
            onlineUsers.remove(userId);
        }
        
        return isOnline;
    }
    
    /**
     * Get user's last seen timestamp
     */
    public LocalDateTime getLastSeen(UUID userId) {
        return onlineUsers.get(userId);
    }
    
    /**
     * Broadcast online status to users who might be interested
     * (e.g., users in the same chat conversation)
     */
    private void broadcastOnlineStatus(UUID userId, boolean isOnline) {
        try {
            Map<String, Object> statusUpdate = Map.of(
                "userId", userId.toString(),
                "isOnline", isOnline,
                "timestamp", LocalDateTime.now().toString()
            );
            
            // Broadcast to topic for online status updates
            messagingTemplate.convertAndSend("/topic/online-status", statusUpdate);
            
            // Also send to user-specific queue for their own status
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/online-status",
                statusUpdate
            );
            
            log.debug("Broadcasted online status for user {}: {}", userId, isOnline);
        } catch (Exception e) {
            log.error("Error broadcasting online status: {}", e.getMessage());
        }
    }
    
    /**
     * Clean up stale online statuses (users who haven't sent heartbeat)
     */
    public void cleanupStaleStatuses() {
        LocalDateTime now = LocalDateTime.now();
        onlineUsers.entrySet().removeIf(entry -> {
            LocalDateTime timeout = entry.getValue().plusMinutes(OFFLINE_TIMEOUT_MINUTES);
            if (now.isAfter(timeout)) {
                log.debug("Removing stale online status for user: {}", entry.getKey());
                broadcastOnlineStatus(entry.getKey(), false);
                return true;
            }
            return false;
        });
    }
}

