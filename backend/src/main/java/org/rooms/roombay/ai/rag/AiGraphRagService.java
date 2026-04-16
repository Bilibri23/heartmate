package org.rooms.roombay.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.AiGraphStatsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid retrieval: vector seed chunks + graph neighborhood expansion + score merge.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiGraphRagService {

    private final AiRagRepository ragRepository;

    @Value("${roombay.ai.graph-rag-enabled:true}")
    private boolean graphRagEnabledFlag;

    @Value("${roombay.ai.graph-rag-seed-k:8}")
    private int seedK;

    @Value("${roombay.ai.graph-rag-final-k:8}")
    private int finalK;

    @Value("${roombay.ai.graph-rag-expand-limit:16}")
    private int expandLimit;

    @Value("${roombay.ai.graph-vector-weight:0.65}")
    private double vectorWeight;

    @Value("${roombay.ai.graph-edge-weight:0.35}")
    private double graphWeight;

    public boolean isGraphRagEnabled() {
        return graphRagEnabledFlag;
    }

    public AiGraphStatsResponse getGraphStats() {
        Map<String, Long> g = ragRepository.graphStats();
        return AiGraphStatsResponse.builder()
                .documentCount(ragRepository.countDocuments())
                .chunkCount(ragRepository.countChunks())
                .entityCount(g.getOrDefault("entityCount", 0L))
                .edgeCount(g.getOrDefault("edgeCount", 0L))
                .chunkEntityLinkCount(g.getOrDefault("chunkEntityLinkCount", 0L))
                .graphRagEnabled(graphRagEnabledFlag)
                .build();
    }

    /**
     * Returns up to {@code finalK} chunks, ranked by hybrid score. Falls back to flat vector list if graph is empty.
     */
    public List<AiRagRepository.ChunkRow> retrieve(List<Double> queryEmbedding, int requestedK) {
        int k = requestedK > 0 ? requestedK : finalK;
        if (!graphRagEnabledFlag) {
            return ragRepository.topKSimilar(queryEmbedding, k);
        }
        List<AiRagRepository.SimilarChunk> seeds = ragRepository.topKSimilarWithDistance(queryEmbedding, Math.max(seedK, k));
        if (seeds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Double> scores = new HashMap<>();
        Set<UUID> seedIds = new LinkedHashSet<>();
        for (AiRagRepository.SimilarChunk sc : seeds) {
            UUID id = sc.getChunk().getId();
            seedIds.add(id);
            double dist = sc.getDistance();
            double vecScore = 1.0 / (1.0 + dist);
            scores.merge(id, vectorWeight * vecScore, Double::sum);
        }

        List<UUID> entityIds = ragRepository.findEntityIdsForChunkIds(seedIds);
        if (entityIds.isEmpty()) {
            return seeds.stream().map(AiRagRepository.SimilarChunk::getChunk).limit(k).collect(Collectors.toList());
        }

        List<UUID> neighborIds = ragRepository.findNeighborChunkIds(seedIds, entityIds, expandLimit).stream()
                .filter(nid -> !seedIds.contains(nid))
                .toList();
        double rank = 1.0;
        for (UUID nid : neighborIds) {
            double g = graphWeight * (rank / (neighborIds.size() + 1.0));
            scores.merge(nid, g, Double::sum);
            rank += 1.0;
        }

        List<UUID> orderedIds = scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(k)
                .toList();

        Map<UUID, AiRagRepository.ChunkRow> byId = new LinkedHashMap<>();
        for (AiRagRepository.SimilarChunk sc : seeds) {
            byId.putIfAbsent(sc.getChunk().getId(), sc.getChunk());
        }
        for (AiRagRepository.ChunkRow row : ragRepository.loadChunksByIds(orderedIds)) {
            byId.put(row.getId(), row);
        }

        List<AiRagRepository.ChunkRow> out = new ArrayList<>();
        for (UUID id : orderedIds) {
            AiRagRepository.ChunkRow row = byId.get(id);
            if (row != null) {
                out.add(row);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("[AI] GraphRAG seeds={} entities={} neighbors={} out={}", seeds.size(), entityIds.size(), neighborIds.size(), out.size());
        }
        return out;
    }
}
