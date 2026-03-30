package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {
    private Long totalUsers;
    private Long totalStudents;
    private Long totalLandlords;
    private Long verifiedStudents;
    private Long activeListings;
    private Long pendingListings;
    private Long totalMatches;
    private Long acceptedMatches;
    private Long pendingVerifications;
    private Long pendingListingApprovals;
}

