package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGraphStatsResponse {
    private long documentCount;
    private long chunkCount;
    private long entityCount;
    private long edgeCount;
    private long chunkEntityLinkCount;
    private boolean graphRagEnabled;
}
