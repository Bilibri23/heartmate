package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    
    private UUID id;
    private UUID senderId;
    private String senderFirstName;
    private String senderLastName;
    private String senderProfilePhoto;
    private UUID receiverId;
    private String receiverFirstName;
    private String receiverLastName;
    private String receiverProfilePhoto;
    private String content;
    private String messageType;
    private SharedListingInfo sharedListing;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private Boolean isSentByMe;
    private Boolean isEdited;
    private LocalDateTime editedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedListingInfo {
        private UUID id;
        private String title;
        private String address;
        private Integer rentAmount;
        private String primaryPhotoUrl;
        private String status;
    }
}
