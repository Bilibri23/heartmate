package org.rooms.roombay.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.ListingResponse;
import org.rooms.roombay.service.AppErrorLogService;
import org.rooms.roombay.service.OnlineStatusService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final OnlineStatusService onlineStatusService;
    private final AppErrorLogService appErrorLogService;
    
    /**
     * Broadcast listing update to all subscribers
     */
    public void broadcastListingUpdate(ListingResponse listing) {
        log.info("Broadcasting listing update for listing: {}", listing.getId());
        messagingTemplate.convertAndSend("/topic/listings", listing);
    }
    
    /**
     * Broadcast new listing to all subscribers
     */
    public void broadcastNewListing(ListingResponse listing) {
        log.info("Broadcasting new listing: {}", listing.getId());
        messagingTemplate.convertAndSend("/topic/listings/new", listing);
    }
    
    /**
     * Send notification to specific user
     */
    public void sendToUser(String userId, String destination, Object payload) {
        log.info("Sending message to user: {} at destination: {}", userId, destination);
        messagingTemplate.convertAndSendToUser(userId, destination, payload);
    }
    
    /**
     * Handle incoming messages from clients
     */
    @MessageMapping("/listings/subscribe")
    @SendTo("/topic/listings")
    public Map<String, String> handleSubscription(Map<String, String> message) {
        log.info("Client subscribed to listings updates: {}", message);
        return Map.of("status", "subscribed", "message", "Successfully subscribed to listing updates");
    }
    
    /**
     * Handle user heartbeat to maintain online status
     */
    @MessageMapping("/online/heartbeat")
    public void handleHeartbeat(Map<String, String> message) {
        try {
            String userIdStr = message.get("userId");
            if (userIdStr != null) {
                UUID userId = UUID.fromString(userIdStr);
                onlineStatusService.updateLastSeen(userId);
            }
        } catch (Exception e) {
            log.error("Error handling heartbeat: {}", e.getMessage());
            appErrorLogService.log("WARN", "WEBSOCKET", "WebSocket heartbeat failed: " + e.getMessage(), "/ws/online/heartbeat", e);
        }
    }
    
    /**
     * Handle user going online
     */
    @MessageMapping("/online/connect")
    public void handleUserConnect(Map<String, String> message) {
        try {
            String userIdStr = message.get("userId");
            if (userIdStr != null) {
                UUID userId = UUID.fromString(userIdStr);
                onlineStatusService.markUserOnline(userId);
            }
        } catch (Exception e) {
            log.error("Error handling user connect: {}", e.getMessage());
            appErrorLogService.log("WARN", "WEBSOCKET", "WebSocket user connect failed: " + e.getMessage(), "/ws/online/connect", e);
        }
    }
    
    /**
     * Handle user going offline
     */
    @MessageMapping("/online/disconnect")
    public void handleUserDisconnect(Map<String, String> message) {
        try {
            String userIdStr = message.get("userId");
            if (userIdStr != null) {
                UUID userId = UUID.fromString(userIdStr);
                onlineStatusService.markUserOffline(userId);
            }
        } catch (Exception e) {
            log.error("Error handling user disconnect: {}", e.getMessage());
            appErrorLogService.log("WARN", "WEBSOCKET", "WebSocket user disconnect failed: " + e.getMessage(), "/ws/online/disconnect", e);
        }
    }
    
    /**
     * WebSocket connection established
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("WebSocket connection established: {}", event.getMessage());
    }
    
    /**
     * WebSocket connection closed
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info("WebSocket connection closed: {}", event.getMessage());
        // Note: User ID extraction from session would require additional setup
        // For now, we rely on explicit disconnect messages from clients
    }
}
