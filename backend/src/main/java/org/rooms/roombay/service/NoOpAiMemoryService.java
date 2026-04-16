package org.rooms.roombay.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(value = "roombay.ai.memory.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAiMemoryService implements AiMemoryService {

    @Override
    public List<MemoryTurn> loadRecentTurns(UUID userId, String threadId, int limit) {
        return List.of();
    }

    @Override
    public void appendTurn(UUID userId, String threadId, String userMessage, String assistantAnswer) {
        // No-op fallback keeps chat available when memory is disabled/unconfigured.
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
