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
    /** Populated when the LangGraph orchestrator handled the request. */
    private OrchestratorMeta meta;
    /** Write-tool results from the agentic orchestrator (favorites, applications, visits). */
    private List<ToolExecution> toolExecutions;
    /** Queue items (e.g. pending applications/listings) each with their own confirm-action buttons. */
    private List<ActionItem> actionItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        private String id;
        private String title;
        private String subtitle;
        private List<SuggestedAction> actions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrchestratorMeta {
        private String intent;
        private Integer groundingRetries;
        private Integer listingRetries;
        private Boolean regenerated;
        private Boolean safetyBlocked;
        private Integer latencyMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolExecution {
        private String tool;
        private Boolean success;
        private String message;
        private String entityId;
    }

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
        private String type; // NAVIGATE | COPY_TEXT | CONFIRM_ACTION
        private String actionUrl;
        private String copyText;
        /** For CONFIRM_ACTION: the privileged tool to run when the user clicks confirm. */
        private String tool;
        /** For CONFIRM_ACTION: parameters (e.g. targetId, reason) passed to the execute endpoint. */
        private java.util.Map<String, String> actionParams;
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
        private String listingPurpose;
        private String stayType;
        private String pricePeriod;
        private Integer salePrice;
        private String furnishingStatus;
        private Boolean isSharedAccommodation;
        private Boolean roommatesWanted;
        private Integer availableRoommateSlots;
        private Boolean roomPreviewEnabled;
        private String roomPreviewStatus;
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
