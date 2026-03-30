package org.rooms.roombay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeFeedResponse {
    private FeedSectionResponse forYou;
    private FeedSectionResponse trending;
    private FeedSectionResponse recent;
    private FeedSectionResponse reels;
    /** Active listings that include a video tour or embed (for client reels gating). */
    private Long videoTourListingCount;
}
