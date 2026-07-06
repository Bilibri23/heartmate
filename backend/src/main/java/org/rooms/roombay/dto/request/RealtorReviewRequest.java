package org.rooms.roombay.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin reject/suspend a realtor — reason is surfaced to the realtor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtorReviewRequest {

    private String reason;
}
