package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NoOpAiMemoryServiceTest {

    @Test
    void noOpServiceAlwaysReturnsEmptyAndDisabled() {
        NoOpAiMemoryService service = new NoOpAiMemoryService();

        List<AiMemoryService.MemoryTurn> turns = service.loadRecentTurns(UUID.randomUUID(), "thread-1", 6);

        assertEquals(List.of(), turns);
        assertFalse(service.isEnabled());
        // Should not throw.
        service.appendTurn(UUID.randomUUID(), "thread-1", "hello", "world");
    }
}
