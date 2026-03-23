package org.rooms.roombuddy.dto.response;

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
}

