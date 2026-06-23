package org.rooms.roombay.controller;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.ai.AiModelRouter;
import org.rooms.roombay.ai.rag.AiGraphRagService;
import org.rooms.roombay.ai.rag.AiRagRepository;
import org.rooms.roombay.ai.safety.AiContextSanitizer;
import org.rooms.roombay.ai.safety.AiRetrievalPolicy;
import org.rooms.roombay.repository.ListingPreferencesRepository;
import org.rooms.roombay.service.AiAgentToolService;
import org.rooms.roombay.service.AiCopilotToolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        AiAgentToolService agentTools = mock(AiAgentToolService.class);
        ListingPreferencesRepository prefs = mock(ListingPreferencesRepository.class);

        when(router.embed(any())).thenReturn(List.of(0.1, 0.2));
        when(graph.retrieveWithScores(any(), anyInt())).thenReturn(List.of());
        when(policy.filter(any(), any())).thenReturn(List.of());
        when(sanitizer.sanitize(any())).thenAnswer(inv -> inv.getArgument(0));

        AiInternalToolController controller = new AiInternalToolController(
                router, graph, policy, sanitizer, copilot, agentTools, prefs);
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

    @Test
    void retrieveReturnsRealScores() {
        AiModelRouter router = mock(AiModelRouter.class);
        AiGraphRagService graph = mock(AiGraphRagService.class);
        AiRetrievalPolicy policy = mock(AiRetrievalPolicy.class);
        AiContextSanitizer sanitizer = mock(AiContextSanitizer.class);
        AiCopilotToolService copilot = mock(AiCopilotToolService.class);
        AiAgentToolService agentTools = mock(AiAgentToolService.class);
        ListingPreferencesRepository prefs = mock(ListingPreferencesRepository.class);

        UUID chunkId = UUID.randomUUID();
        AiRagRepository.ChunkRow row = AiRagRepository.ChunkRow.builder()
                .id(chunkId)
                .source("docs/verify.md")
                .title("Verify")
                .chunkText("Use the dashboard.")
                .build();
        when(router.embed(any())).thenReturn(List.of(0.1));
        when(graph.retrieveWithScores(any(), anyInt())).thenReturn(List.of(
                AiGraphRagService.ScoredChunk.builder().chunk(row).score(0.82).build()));
        when(policy.filter(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(sanitizer.sanitize(any())).thenAnswer(inv -> inv.getArgument(0));

        AiInternalToolController controller = new AiInternalToolController(
                router, graph, policy, sanitizer, copilot, agentTools, prefs);
        ReflectionTestUtils.setField(controller, "internalToken", "secret");
        ReflectionTestUtils.setField(controller, "chatRetrievalK", 6);

        ResponseEntity<Map<String, Object>> res = controller.retrieve("secret", Map.of("message", "verify", "role", "STUDENT"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) res.getBody().get("chunks");
        assertEquals(0.82, chunks.get(0).get("score"));
    }

    @Test
    void executeToolRequiresAuth() {
        AiInternalToolController controller = controllerWithToken("secret");
        ResponseEntity<Map<String, Object>> res = controller.executeTool(null, Map.of(
                "userId", UUID.randomUUID().toString(),
                "tool", "SAVE_LISTING_FAVORITE",
                "params", Map.of("listingId", UUID.randomUUID().toString())));
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void executeToolDelegatesToAgentService() {
        AiModelRouter router = mock(AiModelRouter.class);
        AiGraphRagService graph = mock(AiGraphRagService.class);
        AiRetrievalPolicy policy = mock(AiRetrievalPolicy.class);
        AiContextSanitizer sanitizer = mock(AiContextSanitizer.class);
        AiCopilotToolService copilot = mock(AiCopilotToolService.class);
        AiAgentToolService agentTools = mock(AiAgentToolService.class);
        ListingPreferencesRepository prefs = mock(ListingPreferencesRepository.class);

        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(agentTools.execute(any(), any(), any(), any(), any())).thenReturn(
                new AiAgentToolService.ToolExecutionResult("SAVE_LISTING_FAVORITE", true, "Saved", listingId.toString()));

        AiInternalToolController controller = new AiInternalToolController(
                router, graph, policy, sanitizer, copilot, agentTools, prefs);
        ReflectionTestUtils.setField(controller, "internalToken", "secret");

        ResponseEntity<Map<String, Object>> res = controller.executeTool("secret", Map.of(
                "userId", userId.toString(),
                "role", "STUDENT",
                "tool", "SAVE_LISTING_FAVORITE",
                "params", Map.of("listingId", listingId.toString()),
                "requestId", "req-1"));

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue((Boolean) res.getBody().get("success"));
        assertEquals("Saved", res.getBody().get("message"));
    }
}
