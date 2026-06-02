package org.rooms.roombay.ai.safety;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.ai.rag.AiRagRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiRetrievalPolicyTest {

    private final AiRetrievalPolicy policy = new AiRetrievalPolicy();

    @Test
    void tenantCanOnlySeePublicAndTenantDocs() {
        List<AiRagRepository.ChunkRow> out = policy.filter(sampleChunks(), "STUDENT");

        assertThat(out).extracting(AiRagRepository.ChunkRow::getSource)
                .containsExactly("docs/public/platform-overview.md", "docs/tenant/applications.md");
    }

    @Test
    void landlordCanOnlySeePublicAndLandlordDocs() {
        List<AiRagRepository.ChunkRow> out = policy.filter(sampleChunks(), "LANDLORD");

        assertThat(out).extracting(AiRagRepository.ChunkRow::getSource)
                .containsExactly("docs/public/platform-overview.md", "docs/landlord/listings.md");
    }

    @Test
    void adminCanSeeEveryCorpus() {
        List<AiRagRepository.ChunkRow> out = policy.filter(sampleChunks(), "ADMIN");

        assertThat(out).hasSize(6);
    }

    private static List<AiRagRepository.ChunkRow> sampleChunks() {
        return List.of(
                row("docs/public/platform-overview.md"),
                row("docs/tenant/applications.md"),
                row("docs/landlord/listings.md"),
                row("docs/admin/moderation.md"),
                row("docs/internal/ai-errors.md"),
                row("docs/security/security-hardening.md")
        );
    }

    private static AiRagRepository.ChunkRow row(String source) {
        return AiRagRepository.ChunkRow.builder()
                .id(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .chunkIndex(0)
                .chunkText("RoomBay context")
                .source(source)
                .title(source)
                .build();
    }
}
