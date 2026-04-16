package org.rooms.roombay.ai.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGraphRagServiceTest {

    @Mock
    private AiRagRepository ragRepository;

    private AiGraphRagService service;

    @BeforeEach
    void setUp() {
        service = new AiGraphRagService(ragRepository);
        ReflectionTestUtils.setField(service, "graphRagEnabledFlag", true);
        ReflectionTestUtils.setField(service, "seedK", 8);
        ReflectionTestUtils.setField(service, "finalK", 8);
        ReflectionTestUtils.setField(service, "expandLimit", 16);
        ReflectionTestUtils.setField(service, "vectorWeight", 0.65);
        ReflectionTestUtils.setField(service, "graphWeight", 0.35);
    }

    @Test
    void whenGraphDisabled_usesFlatVectorOnly() {
        ReflectionTestUtils.setField(service, "graphRagEnabledFlag", false);
        List<Double> q = List.of(0.1, 0.2);
        UUID id = UUID.randomUUID();
        AiRagRepository.ChunkRow row = AiRagRepository.ChunkRow.builder()
                .id(id)
                .documentId(UUID.randomUUID())
                .chunkIndex(0)
                .chunkText("x")
                .source("docs/a.md")
                .title("A")
                .build();
        when(ragRepository.topKSimilar(q, 4)).thenReturn(List.of(row));

        List<AiRagRepository.ChunkRow> out = service.retrieve(q, 4);
        assertEquals(1, out.size());
        assertEquals(id, out.get(0).getId());
    }

    @Test
    void hybridRetrievalMergesNeighborChunks() {
        UUID seedId = UUID.randomUUID();
        UUID neighborId = UUID.randomUUID();
        AiRagRepository.ChunkRow seed = AiRagRepository.ChunkRow.builder()
                .id(seedId)
                .documentId(UUID.randomUUID())
                .chunkIndex(0)
                .chunkText("seed")
                .source("docs/a.md")
                .title("A")
                .build();
        AiRagRepository.ChunkRow neighbor = AiRagRepository.ChunkRow.builder()
                .id(neighborId)
                .documentId(UUID.randomUUID())
                .chunkIndex(1)
                .chunkText("neighbor")
                .source("docs/b.md")
                .title("B")
                .build();

        when(ragRepository.topKSimilarWithDistance(anyList(), anyInt())).thenReturn(List.of(
                AiRagRepository.SimilarChunk.builder().chunk(seed).distance(0.1).build()
        ));
        when(ragRepository.findEntityIdsForChunkIds(any())).thenReturn(List.of(UUID.randomUUID()));
        when(ragRepository.findNeighborChunkIds(any(), any(), anyInt())).thenReturn(List.of(neighborId));
        when(ragRepository.loadChunksByIds(any())).thenReturn(List.of(seed, neighbor));

        List<AiRagRepository.ChunkRow> out = service.retrieve(List.of(0.1, 0.2), 8);
        assertFalse(out.isEmpty());
        assertEquals(2, out.size());
    }
}
