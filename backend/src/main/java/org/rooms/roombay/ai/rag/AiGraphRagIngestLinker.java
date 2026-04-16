package org.rooms.roombay.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists {@link AiMarkdownGraphExtractor} output into {@link AiRagRepository} graph tables.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiGraphRagIngestLinker {

    private final AiRagRepository ragRepository;

    public Stats linkChunk(UUID documentId, String source, UUID chunkId, String chunkText) {
        AiMarkdownGraphExtractor.ExtractedGraph graph = AiMarkdownGraphExtractor.extract(chunkText, source);
        Map<String, UUID> keyToId = new HashMap<>();
        int links = 0;
        for (AiMarkdownGraphExtractor.EntitySpec e : graph.entities()) {
            UUID id = ragRepository.upsertEntity(
                    e.canonicalKey(),
                    e.type(),
                    e.label(),
                    documentId,
                    Map.of("source", source)
            );
            keyToId.put(e.canonicalKey(), id);
            ragRepository.linkChunkToEntity(chunkId, id, e.weight());
            links++;
        }
        int edgeCount = 0;
        for (AiMarkdownGraphExtractor.EdgeSpec ed : graph.edges()) {
            UUID s = keyToId.get(ed.srcKey());
            UUID t = keyToId.get(ed.dstKey());
            if (s == null || t == null || s.equals(t)) {
                continue;
            }
            ragRepository.mergeEdge(s, t, ed.relationType(), ed.weight(), Map.of());
            edgeCount++;
        }
        if (log.isTraceEnabled()) {
            log.trace("[AI] Graph ingest chunk={} entities={} edges={}", chunkId, keyToId.size(), edgeCount);
        }
        return new Stats(keyToId.size(), edgeCount, links);
    }

    public record Stats(int entities, int edges, int chunkLinks) {
    }
}
