package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.Household;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdResponse {
    
    private UUID id;
    private String name;
    private String description;
    private Household.HouseholdStatus status;
    private UUID leaseId;
    private UUID listingId;
    private String listingTitle;
    private String listingAddress;
    private List<MemberResponse> members;
    private LocalDateTime createdAt;
    
    // Summary stats
    private Long totalExpensesThisMonth;
    private Long myBalanceOwed;
    private Long myBalanceOwedToMe;
    private Integer openTasksCount;
    private Integer openIssuesCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberResponse {
        private UUID id;
        private UUID userId;
        private String firstName;
        private String lastName;
        private String email;
        private String photoUrl;
        private String role;
        private Integer rentSharePercentage;
        private LocalDateTime joinedAt;
        private String inviteStatus;
    }
}
