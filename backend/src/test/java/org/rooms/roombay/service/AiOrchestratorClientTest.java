package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.dto.request.AiChatRequest;
import org.rooms.roombay.dto.response.AiChatResponse;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiOrchestratorClientTest {

    @Test
    void returnsEmptyWhenSidecarUnreachable() {
        // Points at a closed port: orchestrate must swallow the error and signal fallback.
        AiOrchestratorClient client = new AiOrchestratorClient("http://127.0.0.1:1", 1000, "token");
        AiChatRequest request = AiChatRequest.builder()
                .message("find a studio in Damas")
                .persona(AiChatRequest.Persona.TENANT)
                .build();

        Optional<AiChatResponse> result = client.orchestrate(UUID.randomUUID(), "STUDENT", request, "thread-1", "req-1");

        assertTrue(result.isEmpty(), "Unreachable sidecar should fall back (empty optional)");
    }
}
