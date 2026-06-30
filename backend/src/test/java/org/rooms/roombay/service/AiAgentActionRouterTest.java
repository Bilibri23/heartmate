package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.request.AiChatRequest;
import org.rooms.roombay.dto.response.AiChatResponse;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAgentActionRouterTest {

    @Mock
    private AiAgentToolService aiAgentToolService;

    private AiAgentActionRouter router() {
        return new AiAgentActionRouter(aiAgentToolService);
    }

    private static AiChatRequest msg(String text) {
        return AiChatRequest.builder().message(text).persona(AiChatRequest.Persona.TENANT).build();
    }

    @Test
    void saveIntentWithExplicitUuidExecutesFavoriteTool() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(aiAgentToolService.execute(eq(userId), eq("STUDENT"), eq("SAVE_LISTING_FAVORITE"), anyMap(), anyString()))
                .thenReturn(new AiAgentToolService.ToolExecutionResult(
                        "SAVE_LISTING_FAVORITE", true, "Saved Cozy Studio to your favorites.", listingId.toString()));

        Optional<AiChatResponse> out = router().tryExecute(
                userId, "STUDENT", msg("please save this listing " + listingId), "t1");

        assertThat(out).isPresent();
        assertThat(out.get().getToolExecutions()).hasSize(1);
        assertThat(out.get().getToolExecutions().get(0).getSuccess()).isTrue();
        assertThat(out.get().getAnswer()).contains("Saved");

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(aiAgentToolService).execute(eq(userId), eq("STUDENT"), eq("SAVE_LISTING_FAVORITE"), params.capture(), anyString());
        assertThat(params.getValue()).containsEntry("listingId", listingId.toString());
    }

    @Test
    void applyIntentExecutesApplyToolAndPassesMessage() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(aiAgentToolService.execute(eq(userId), eq("STUDENT"), eq("APPLY_TO_LISTING"), anyMap(), anyString()))
                .thenReturn(new AiAgentToolService.ToolExecutionResult(
                        "APPLY_TO_LISTING", true, "Application submitted for Cozy Studio.", "app-1"));

        Optional<AiChatResponse> out = router().tryExecute(
                userId, "STUDENT", msg("apply to this listing " + listingId), "t1");

        assertThat(out).isPresent();
        assertThat(out.get().getToolExecutions().get(0).getTool()).isEqualTo("APPLY_TO_LISTING");

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(aiAgentToolService).execute(eq(userId), eq("STUDENT"), eq("APPLY_TO_LISTING"), params.capture(), anyString());
        assertThat(params.getValue()).containsKey("message");
    }

    @Test
    void visitIntentExecutesRequestVisitTool() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(aiAgentToolService.execute(eq(userId), eq("STUDENT"), eq("REQUEST_VISIT"), anyMap(), anyString()))
                .thenReturn(new AiAgentToolService.ToolExecutionResult(
                        "REQUEST_VISIT", true, "Visit requested for Cozy Studio.", "visit-1"));

        Optional<AiChatResponse> out = router().tryExecute(
                userId, "STUDENT", msg("can you schedule a viewing for " + listingId), "t1");

        assertThat(out).isPresent();
        assertThat(out.get().getToolExecutions().get(0).getTool()).isEqualTo("REQUEST_VISIT");
    }

    @Test
    void contextListingIdResolvesTargetWhenMessageHasNoUuid() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(aiAgentToolService.execute(eq(userId), eq("STUDENT"), eq("SAVE_LISTING_FAVORITE"), anyMap(), anyString()))
                .thenReturn(new AiAgentToolService.ToolExecutionResult(
                        "SAVE_LISTING_FAVORITE", true, "Saved.", listingId.toString()));

        AiChatRequest request = AiChatRequest.builder()
                .message("save this one")
                .persona(AiChatRequest.Persona.TENANT)
                .contextListingId(listingId.toString())
                .build();

        Optional<AiChatResponse> out = router().tryExecute(userId, "STUDENT", request, "t1");

        assertThat(out).isPresent();
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(aiAgentToolService).execute(eq(userId), eq("STUDENT"), eq("SAVE_LISTING_FAVORITE"), params.capture(), anyString());
        assertThat(params.getValue()).containsEntry("listingId", listingId.toString());
    }

    @Test
    void landlordCannotRunTenantActions() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        Optional<AiChatResponse> out = router().tryExecute(
                userId, "LANDLORD", msg("apply to this listing " + listingId), "t1");

        assertThat(out).isPresent();
        assertThat(out.get().getToolExecutions()).isEmpty();
        assertThat(out.get().getAnswer()).contains("tenant");
        verifyNoInteractions(aiAgentToolService);
    }

    @Test
    void howToQuestionFallsThroughToRagPipeline() {
        Optional<AiChatResponse> out = router().tryExecute(
                UUID.randomUUID(), "STUDENT", msg("how do I apply to a listing?"), "t1");

        assertThat(out).isEmpty();
        verifyNoInteractions(aiAgentToolService);
    }

    @Test
    void actionWithoutResolvableListingAsksWhichListing() {
        Optional<AiChatResponse> out = router().tryExecute(
                UUID.randomUUID(), "STUDENT", msg("apply to this listing for me"), "t1");

        assertThat(out).isPresent();
        assertThat(out.get().getToolExecutions()).isEmpty();
        assertThat(out.get().getAnswer().toLowerCase()).contains("which listing");
        verify(aiAgentToolService, never()).execute(any(UUID.class), anyString(), anyString(), anyMap(), anyString());
    }

    @Test
    void nonActionMessageReturnsEmpty() {
        Optional<AiChatResponse> out = router().tryExecute(
                UUID.randomUUID(), "STUDENT", msg("what neighborhoods are safe in Yaoundé?"), "t1");

        assertThat(out).isEmpty();
        verifyNoInteractions(aiAgentToolService);
    }
}
