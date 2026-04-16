package org.rooms.roombay.service;

import java.util.List;
import java.util.UUID;

public interface AiMemoryService {

    List<MemoryTurn> loadRecentTurns(UUID userId, String threadId, int limit);

    void appendTurn(UUID userId, String threadId, String userMessage, String assistantAnswer);

    default boolean isEnabled() {
        return true;
    }

    record MemoryTurn(String userMessage, String assistantAnswer) {}
}
