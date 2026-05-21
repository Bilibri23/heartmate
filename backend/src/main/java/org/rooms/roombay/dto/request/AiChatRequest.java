package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Persona is required")
    private Persona persona;

    /**
     * Optional conversation thread identifier for short-term memory continuity.
     * When absent, the server generates a new thread id and returns it in the response.
     */
    private String threadId;

    public enum Persona {
        TENANT,
        LANDLORD,
        /** Platform administrator; server forces this when JWT role is ADMIN. */
        ADMIN
    }
}

