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
    private String threadId;
    private List<Citation> citations;
    private List<SuggestedAction> suggestedActions;
    private List<ListingResult> listingResults;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListingResult {
        private String id;
        private String title;
        private Integer rentAmount;
        private String city;
        private String neighborhood;
        private String propertyType;
        private Boolean verified;
        private String status;
        private Boolean available;
        private String thumbnailUrl;
        private String landlordId;
        private String matchLabel;
        private String matchReason;
        private List<String> whyThisMatches;
        private List<SuggestedAction> actions;
    }
}
