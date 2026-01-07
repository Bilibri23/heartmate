package org.rooms.roombuddy.dto.response;

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
public class ConversationResponse {
    
    private UUID userId;
    private String firstName;
    private String lastName;
    private String profilePhotoUrl;
    private String email;
    private Boolean isOnline;
    private MessageResponse lastMessage;
    private Long unreadCount;
    private LocalDateTime lastMessageTime;
}
