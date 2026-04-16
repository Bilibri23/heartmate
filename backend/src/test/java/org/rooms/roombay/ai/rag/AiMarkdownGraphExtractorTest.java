package org.rooms.roombay.ai.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMarkdownGraphExtractorTest {

    @Test
    void extractsHeadingAndTopicEntities() {
        String md = """
                # Product vision

                RoomBay makes discovery as easy as ordering on Amazon. Verification and leases are separate flows.
                """;
        AiMarkdownGraphExtractor.ExtractedGraph g = AiMarkdownGraphExtractor.extract(md, "docs/rag-roombay-platform-usage.md");
        assertTrue(g.entities().stream().anyMatch(e -> "HEADING".equals(e.type())));
        assertTrue(g.entities().stream().anyMatch(e -> "TOPIC".equals(e.type()) && e.label().contains("verification")));
        assertTrue(g.edges().stream().anyMatch(e -> "CO_OCCURS".equals(e.relationType())));
    }

    @Test
    void extractsMarkdownLinksToDocs() {
        String md = """
                # Related docs

                See also [backlog](docs/PRODUCT-BACKLOG.md) for priorities.
                """;
        AiMarkdownGraphExtractor.ExtractedGraph g = AiMarkdownGraphExtractor.extract(md, "docs/README.md");
        assertTrue(g.entities().stream().anyMatch(e -> "DOC_REF".equals(e.type())));
        assertTrue(g.edges().stream().anyMatch(e -> "REFERENCES".equals(e.relationType())));
    }
}
