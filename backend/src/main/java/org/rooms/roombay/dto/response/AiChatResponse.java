package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private String answer;
    private List<Citation> citations;
    private List<SuggestedAction> suggestedActions;
    /** False when no RAG chunks matched (ingest missing, dimension mismatch, or irrelevant query). */
    private Boolean ragGrounded;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private String chunkId;
        private String source;
        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestedAction {
        private String id;
        private String label;
        private String type; // NAVIGATE | COPY_TEXT
        private String actionUrl;
        private String copyText;
    }
}

