package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    
    @NotNull(message = "Receiver ID is required")
    private UUID receiverId;
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    private String messageType; // TEXT, LISTING_SHARE
    
    private UUID sharedListingId; // For LISTING_SHARE type
}
