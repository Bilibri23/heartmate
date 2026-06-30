package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssistantServiceTest {

    private final AiAssistantService service = new AiAssistantService(
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null
    );

    @Test
    void noDocFallbackUsesCanonicalGroundedLanguage() {
        assertThat(AiAssistantService.NO_DOC_FALLBACK_ANSWER)
                .isEqualTo("I do not have documentation that answers this question. Please open the relevant RoomBay screen or contact support.");
    }

    @Test
    void parserKeepsPlainMarkdownAnswers() {
        Object parsed = ReflectionTestUtils.invokeMethod(service, "parseAssistantOutput",
                "RoomBay listings show trust signals when review information is available.\n\nNext step: open the listing.");

        assertThat(answer(parsed)).contains("RoomBay listings show trust signals");
        assertThat(actions(parsed)).isEqualTo(0);
    }

    @Test
    void parserSeparatesSuggestedActionsMarker() {
        Object parsed = ReflectionTestUtils.invokeMethod(service, "parseAssistantOutput",
                "Use the Landlord dashboard to review pending applications.\nSUGGESTED_ACTIONS_JSON: [{\"id\":\"apps\",\"label\":\"Open applications\",\"type\":\"NAVIGATE\",\"actionUrl\":\"/landlord/applications\"}]");

        assertThat(answer(parsed)).isEqualTo("Use the Landlord dashboard to review pending applications.");
        assertThat(actions(parsed)).isEqualTo(1);
    }

    @Test
    void parserReadsJsonObjectResponses() {
        Object parsed = ReflectionTestUtils.invokeMethod(service, "parseAssistantOutput",
                "{\"answer\":\"Open Applications to review the current status.\",\"suggestedActions\":[{\"id\":\"applications\",\"label\":\"Open applications\",\"type\":\"NAVIGATE\",\"actionUrl\":\"/applications\"}]}");

        assertThat(answer(parsed)).isEqualTo("Open Applications to review the current status.");
        assertThat(actions(parsed)).isEqualTo(1);
    }

    @Test
    void parserReadsFencedJsonObjectResponses() {
        Object parsed = ReflectionTestUtils.invokeMethod(service, "parseAssistantOutput",
                "```json\n{\"response\":\"RoomBay support can review persistent notification failures.\"}\n```");

        assertThat(answer(parsed)).isEqualTo("RoomBay support can review persistent notification failures.");
    }

    @Test
    void parserLeavesActionOnlyResponsesBlankSoFallbackCanTrigger() {
        Object parsed = ReflectionTestUtils.invokeMethod(service, "parseAssistantOutput",
                "SUGGESTED_ACTIONS_JSON: []");

        assertThat(answer(parsed)).isBlank();
        assertThat(actions(parsed)).isEqualTo(0);
    }

    @Test
    void chunkFallbackBuildsReadableAnswerWhenModelReturnsBlank() {
        var chunk = org.rooms.roombay.ai.rag.AiRagRepository.ChunkRow.builder()
                .title("Admin Incident Triage")
                .chunkText("Admins should start with queue counts, then inspect recent errors.")
                .build();

        String fallback = (String) ReflectionTestUtils.invokeMethod(service, "buildChunkGroundedFallback",
                java.util.List.of(chunk), "incident triage");

        assertThat(fallback).contains("Admin Incident Triage");
        assertThat(fallback).contains("queue counts");
    }

    @Test
    void parserExtractsLeadingProseBeforeJsonMarker() {
        Object parsed = ReflectionTestUtils.invokeMethod(service, "parseAssistantOutput",
                "Open the Admin dashboard to review the queue.\nSUGGESTED_ACTIONS_JSON: []");

        assertThat(answer(parsed)).isEqualTo("Open the Admin dashboard to review the queue.");
    }

    @Test
    void extractLeadingProseSkipsJsonLines() {
        String prose = (String) ReflectionTestUtils.invokeMethod(service, "extractLeadingProse",
                "Use the documented checklist.\n{\"answer\":\"ignored\"}");

        assertThat(prose).isEqualTo("Use the documented checklist.");
    }

    private static String answer(Object parsed) {
        return (String) ReflectionTestUtils.invokeGetterMethod(parsed, "answer");
    }

    private static int actions(Object parsed) {
        java.util.List<?> actions = (java.util.List<?>) ReflectionTestUtils.invokeGetterMethod(parsed, "actions");
        return actions == null ? 0 : actions.size();
    }
}
