package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssistantServiceTest {

    @Test
    void noDocFallbackUsesCanonicalGroundedLanguage() {
        assertThat(AiAssistantService.NO_DOC_FALLBACK_ANSWER)
                .isEqualTo("I do not have documentation that answers this question. Please open the relevant RoomBay screen or contact support.");
    }
}
