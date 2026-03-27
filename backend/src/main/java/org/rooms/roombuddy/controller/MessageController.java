package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.MessageRequest;
import org.rooms.roombay.dto.response.ConversationResponse;
import org.rooms.roombay.dto.response.MessageResponse;
import org.rooms.roombay.security.RequiresCompletion;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Messages", description = "Real-time messaging endpoints")
public class MessageController {
    
    private final MessageService messageService;
    
    /**
     * Send a message
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @RequiresCompletion(operation = "MESSAGE")
    @Operation(summary = "Send a message", description = "Send a message to another user")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request) {
        UUID senderId = SecurityUtils.getCurrentUserId();
        log.info("REST: Sending message from {} to {}", senderId, request.getReceiverId());
        
        MessageResponse response = messageService.sendMessage(senderId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get conversation with a specific user
     */
    @GetMapping("/conversation/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get conversation", description = "Get message history with a specific user")
    public ResponseEntity<Page<MessageResponse>> getConversation(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Getting conversation between {} and {}", currentUserId, userId);
        
        Page<MessageResponse> messages = messageService.getConversation(currentUserId, userId, page, size);
        return ResponseEntity.ok(messages);
    }
    
    /**
     * Get all conversations
     */
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all conversations", description = "Get list of all conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations() {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Getting all conversations for user: {}", userId);
        
        List<ConversationResponse> conversations = messageService.getConversations(userId);
        return ResponseEntity.ok(conversations);
    }
    
    /**
     * Mark messages as read
     */
    @PutMapping("/read/{senderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark as read", description = "Mark all messages from a user as read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID senderId) {
        UUID receiverId = SecurityUtils.getCurrentUserId();
        log.info("Marking messages as read: receiver={}, sender={}", receiverId, senderId);
        
        messageService.markAsRead(receiverId, senderId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get unread message count
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread count", description = "Get total unread message count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        Long count = messageService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
    
    /**
     * Edit a message (sender only)
     */
    @PutMapping("/{messageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Edit message", description = "Edit a message you sent")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable UUID messageId,
            @RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String newContent = body.get("content");
        log.info("Editing message {} for user {}", messageId, userId);
        
        MessageResponse response = messageService.editMessage(messageId, userId, newContent);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a message
     */
    @DeleteMapping("/{messageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete message", description = "Soft delete a message")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Deleting message {} for user {}", messageId, userId);
        
        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Search messages in conversation
     */
    @GetMapping("/search/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search messages", description = "Search messages in a conversation")
    public ResponseEntity<Page<MessageResponse>> searchMessages(
            @PathVariable UUID userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Searching messages between {} and {} for: {}", currentUserId, userId, query);
        
        Page<MessageResponse> messages = messageService.searchMessages(currentUserId, userId, query, page, size);
        return ResponseEntity.ok(messages);
    }
    
    /**
     * WebSocket: Send message
     */
    @MessageMapping("/chat.send")
    public void handleWebSocketMessage(@Payload MessageRequest request) {
        UUID senderId = SecurityUtils.getCurrentUserId();
        log.info("WebSocket: Sending message from {} to {}", senderId, request.getReceiverId());
        messageService.sendMessage(senderId, request);
    }
    
    /**
     * WebSocket: Typing indicator
     */
    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(@Payload Map<String, Object> payload) {
        UUID senderId = SecurityUtils.getCurrentUserId();
        UUID receiverId = UUID.fromString((String) payload.get("receiverId"));
        Boolean isTyping = (Boolean) payload.get("isTyping");
        
        log.info("WebSocket: Typing indicator from {} to {}: {}", senderId, receiverId, isTyping);
        messageService.sendTypingIndicator(senderId, receiverId, isTyping);
    }
}
