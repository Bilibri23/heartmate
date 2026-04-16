package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class LangGraphMemoryServiceTest {

    @Test
    void whenProviderIsNotLangGraph_serviceBehavesAsDisabledNoOp() {
        LangGraphMemoryService service = new LangGraphMemoryService(mock(RestClient.Builder.class));
        ReflectionTestUtils.setField(service, "provider", "custom");

        List<AiMemoryService.MemoryTurn> turns = service.loadRecentTurns(UUID.randomUUID(), "thread-1", 4);

        assertEquals(List.of(), turns);
        assertFalse(service.isEnabled());
        // Should not throw while disabled.
        service.appendTurn(UUID.randomUUID(), "thread-1", "q", "a");
    }
}
