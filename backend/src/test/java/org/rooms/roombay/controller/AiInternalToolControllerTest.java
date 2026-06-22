package org.rooms.roombay.controller;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.ai.AiModelRouter;
import org.rooms.roombay.ai.rag.AiGraphRagService;
import org.rooms.roombay.ai.safety.AiContextSanitizer;
import org.rooms.roombay.ai.safety.AiRetrievalPolicy;
import org.rooms.roombay.repository.ListingPreferencesRepository;
import org.rooms.roombay.service.AiCopilotToolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiInternalToolControllerTest {

    private AiInternalToolController controllerWithToken(String configuredToken) {
        AiModelRouter router = mock(AiModelRouter.class);
        AiGraphRagService graph = mock(AiGraphRagService.class);
        AiRetrievalPolicy policy = mock(AiRetrievalPolicy.class);
        AiContextSanitizer sanitizer = mock(AiContextSanitizer.class);
        AiCopilotToolService copilot = mock(AiCopilotToolService.class);
        ListingPreferencesRepository prefs = mock(ListingPreferencesRepository.class);

        when(router.embed(any())).thenReturn(List.of(0.1, 0.2));
        when(graph.retrieve(any(), anyInt())).thenReturn(List.of());
        when(policy.filter(any(), any())).thenReturn(List.of());

        AiInternalToolController controller = new AiInternalToolController(
                router, graph, policy, sanitizer, copilot, prefs);
        ReflectionTestUtils.setField(controller, "internalToken", configuredToken);
        ReflectionTestUtils.setField(controller, "chatRetrievalK", 6);
        return controller;
    }

    @Test
    void rejectsMissingToken() {
        AiInternalToolController controller = controllerWithToken("secret");
        ResponseEntity<Map<String, Object>> res = controller.retrieve(null, Map.of("message", "hi", "role", "STUDENT"));
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void rejectsWrongToken() {
        AiInternalToolController controller = controllerWithToken("secret");
        ResponseEntity<Map<String, Object>> res = controller.retrieve("nope", Map.of("message", "hi", "role", "STUDENT"));
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void rejectsAllWhenTokenNotConfigured() {
        // Closed by default: a blank configured token must reject even a blank header.
        AiInternalToolController controller = controllerWithToken("");
        ResponseEntity<Map<String, Object>> res = controller.retrieve("", Map.of("message", "hi", "role", "STUDENT"));
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void acceptsCorrectTokenAndReturnsChunks() {
        AiInternalToolController controller = controllerWithToken("secret");
        ResponseEntity<Map<String, Object>> res = controller.retrieve("secret", Map.of("message", "how to verify", "role", "STUDENT"));
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(List.of(), res.getBody().get("chunks"));
    }
}
