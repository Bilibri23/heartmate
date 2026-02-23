package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.MessageRequest;
import org.rooms.roombuddy.dto.response.ConversationResponse;
import org.rooms.roombuddy.dto.response.MessageResponse;
import org.rooms.roombuddy.entity.Message;
import org.rooms.roombuddy.entity.Profile;
import org.rooms.roombuddy.entity.PropertyListing;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.ListingPhotoRepository;
import org.rooms.roombuddy.repository.MessageRepository;
import org.rooms.roombuddy.repository.ProfileRepository;
import org.rooms.roombuddy.repository.PropertyListingRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PropertyListingRepository listingRepository;
    private final ListingPhotoRepository listingPhotoRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    
    /**
     * Send a message
     */
    public MessageResponse sendMessage(UUID senderId, MessageRequest request) {
        log.info("Sending message from {} to {}", senderId, request.getReceiverId());
        
        // Validate sender
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        // Validate receiver
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));
        
        // Prevent self-messaging
        if (senderId.equals(request.getReceiverId())) {
            throw new BadRequestException("Cannot send message to yourself");
        }
        
        // Build message
        Message.MessageBuilder messageBuilder = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .messageType(Message.MessageType.valueOf(
                    request.getMessageType() != null ? request.getMessageType() : "TEXT"
                ));
        
        // Handle listing share
        if (request.getSharedListingId() != null) {
            PropertyListing listing = listingRepository.findById(request.getSharedListingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
            messageBuilder.sharedListing(listing);
            messageBuilder.messageType(Message.MessageType.LISTING_SHARE);
        }
        
        Message message = messageRepository.save(messageBuilder.build());
        
        // Convert to response
        MessageResponse response = toMessageResponse(message, senderId);
        
        // Send real-time notification via WebSocket
        messagingTemplate.convertAndSend(
                "/queue/messages/" + request.getReceiverId(),
                response
        );
        
        // Send persistent notification for new message
        notificationService.notifyNewMessage(
                receiver.getId(),
                message.getId(),
                sender.getFirstName() + " " + sender.getLastName()
        );
        
        log.info("Message sent successfully: {}", message.getId());
        return response;
    }
    
    /**
     * Get conversation between two users
     */
    public Page<MessageResponse> getConversation(UUID userId, UUID otherUserId, int page, int size) {
        log.info("Getting conversation between {} and {}", userId, otherUserId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findConversation(userId, otherUserId, pageable);
        
        return messages.map(m -> toMessageResponse(m, userId));
    }
    
    /**
     * Get all conversations for a user
     */
    public List<ConversationResponse> getConversations(UUID userId) {
        log.info("Getting all conversations for user: {}", userId);
        
        List<Message> lastMessages = messageRepository.findLastMessagesForUser(userId);
        List<ConversationResponse> conversations = new ArrayList<>();
        
        for (Message message : lastMessages) {
            // Determine the other user
            UUID otherUserId = message.getSender().getId().equals(userId) 
                    ? message.getReceiver().getId() 
                    : message.getSender().getId();
            
            User otherUser = message.getSender().getId().equals(userId) 
                    ? message.getReceiver() 
                    : message.getSender();
            
            // Get unread count
            Long unreadCount = messageRepository.countUnreadMessages(otherUserId, userId);
            
            // Build conversation response
            conversations.add(toConversationResponse(otherUser, message, unreadCount, userId));
        }
        
        return conversations;
    }
    
    /**
     * Mark messages as read
     */
    public void markAsRead(UUID receiverId, UUID senderId) {
        log.info("Marking messages as read: receiver={}, sender={}", receiverId, senderId);
        messageRepository.markMessagesAsRead(senderId, receiverId);
        
        // Notify sender via WebSocket that messages were read
        messagingTemplate.convertAndSend(
                "/queue/read-receipts/" + senderId,
                MessageReadReceipt.builder()
                        .readBy(receiverId)
                        .timestamp(java.time.LocalDateTime.now())
                        .build()
        );
    }
    
    /**
     * Get unread message count
     */
    public Long getUnreadCount(UUID userId) {
        return messageRepository.countTotalUnreadMessages(userId);
    }
    
    /**
     * Delete message (soft delete)
     */
    public void deleteMessage(UUID messageId, UUID userId) {
        log.info("Deleting message {} for user {}", messageId, userId);
        
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        // Verify user is part of the conversation
        if (!message.getSender().getId().equals(userId) && 
            !message.getReceiver().getId().equals(userId)) {
            throw new BadRequestException("You don't have permission to delete this message");
        }
        
        // Soft delete
        if (message.getSender().getId().equals(userId)) {
            message.setIsDeletedBySender(true);
        } else {
            message.setIsDeletedByReceiver(true);
        }
        
        messageRepository.save(message);
    }
    
    /**
     * Edit a message (sender only)
     */
    public MessageResponse editMessage(UUID messageId, UUID userId, String newContent) {
        log.info("Editing message {} by user {}", messageId, userId);
        
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        if (!message.getSender().getId().equals(userId)) {
            throw new BadRequestException("Only the sender can edit a message");
        }
        
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be empty");
        }
        
        message.setContent(newContent.trim());
        message.setIsEdited(true);
        message.setEditedAt(java.time.LocalDateTime.now());
        message = messageRepository.save(message);
        
        return toMessageResponse(message, userId);
    }
    
    /**
     * Search messages in conversation
     */
    public Page<MessageResponse> searchMessages(UUID userId, UUID otherUserId, String searchTerm, int page, int size) {
        log.info("Searching messages between {} and {} for: {}", userId, otherUserId, searchTerm);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.searchInConversation(userId, otherUserId, searchTerm, pageable);
        
        return messages.map(m -> toMessageResponse(m, userId));
    }
    
    /**
     * Send typing indicator
     */
    public void sendTypingIndicator(UUID senderId, UUID receiverId, boolean isTyping) {
        messagingTemplate.convertAndSend(
                "/queue/typing/" + receiverId,
                TypingIndicator.builder()
                        .userId(senderId)
                        .isTyping(isTyping)
                        .timestamp(java.time.LocalDateTime.now())
                        .build()
        );
    }
    
    // Helper methods
    
    private MessageResponse toMessageResponse(Message message, UUID currentUserId) {
        Profile senderProfile = profileRepository.findByUserId(message.getSender().getId()).orElse(null);
        Profile receiverProfile = profileRepository.findByUserId(message.getReceiver().getId()).orElse(null);
        
        MessageResponse.MessageResponseBuilder builder = MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderFirstName(message.getSender().getFirstName())
                .senderLastName(message.getSender().getLastName())
                .senderProfilePhoto(senderProfile != null ? senderProfile.getProfilePhotoUrl() : null)
                .receiverId(message.getReceiver().getId())
                .receiverFirstName(message.getReceiver().getFirstName())
                .receiverLastName(message.getReceiver().getLastName())
                .receiverProfilePhoto(receiverProfile != null ? receiverProfile.getProfilePhotoUrl() : null)
                .content(message.getContent())
                .messageType(message.getMessageType().name())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .isSentByMe(message.getSender().getId().equals(currentUserId))
                .isEdited(message.getIsEdited())
                .editedAt(message.getEditedAt());
        
        // Add shared listing info if applicable
        if (message.getSharedListing() != null) {
            PropertyListing listing = message.getSharedListing();
            
            // Fetch primary photo
            String primaryPhotoUrl = listingPhotoRepository
                    .findByListingIdAndIsPrimary(listing.getId(), true)
                    .map(photo -> photo.getPhotoUrl())
                    .orElse(null);
            
            builder.sharedListing(MessageResponse.SharedListingInfo.builder()
                    .id(listing.getId())
                    .title(listing.getTitle())
                    .address(listing.getAddress())
                    .rentAmount(listing.getRentAmount())
                    .primaryPhotoUrl(primaryPhotoUrl)
                    .status(listing.getStatus().name())
                    .build());
        }
        
        return builder.build();
    }
    
    private ConversationResponse toConversationResponse(User otherUser, Message lastMessage, Long unreadCount, UUID currentUserId) {
        Profile profile = profileRepository.findByUserId(otherUser.getId()).orElse(null);
        
        return ConversationResponse.builder()
                .userId(otherUser.getId())
                .firstName(otherUser.getFirstName())
                .lastName(otherUser.getLastName())
                .profilePhotoUrl(profile != null ? profile.getProfilePhotoUrl() : null)
                .email(otherUser.getEmail())
                .isOnline(false) // TODO: Implement online status tracking
                .lastMessage(toMessageResponse(lastMessage, currentUserId))
                .unreadCount(unreadCount)
                .lastMessageTime(lastMessage.getCreatedAt())
                .build();
    }
    
    // Helper DTOs for WebSocket events
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TypingIndicator {
        private UUID userId;
        private Boolean isTyping;
        private java.time.LocalDateTime timestamp;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MessageReadReceipt {
        private UUID readBy;
        private java.time.LocalDateTime timestamp;
    }
}
