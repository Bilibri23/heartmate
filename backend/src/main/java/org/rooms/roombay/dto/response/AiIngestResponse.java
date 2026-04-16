package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiIngestResponse {
    private int documentsProcessed;
    private int chunksInserted;
    private boolean skippedUnchanged;
    /** GraphRAG: entity rows touched during ingest (upserts). */
    private int graphEntitiesWritten;
    /** GraphRAG: edge rows merged during ingest. */
    private int graphEdgesWritten;
}

