package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCompletionResponse {
    private String role;
    private int completionPercentage;
    private List<String> completedSteps;
    private List<String> missingSteps;
    private Map<String, Boolean> operationEligibility;
}
