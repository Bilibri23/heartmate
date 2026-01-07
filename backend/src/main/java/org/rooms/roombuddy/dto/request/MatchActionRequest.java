package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchActionRequest {
    
    @NotBlank(message = "Action is required")
    private String action; // ACCEPT or REJECT
}

