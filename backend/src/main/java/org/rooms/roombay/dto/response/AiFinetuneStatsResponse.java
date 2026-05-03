package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFinetuneStatsResponse {
    private long totalActive;
    private Map<String, Long> byKind;
    private boolean readyForFinetune;
    private int recommendedMinPerKind;
    private String hint;
}
