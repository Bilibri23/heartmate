package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSettingsResponse {
    private Boolean maintenanceMode;
    private Boolean allowNewRegistrations;
    private Boolean requireEmailVerification;
    private Boolean requireTenantVerification;
    private Boolean autoApproveListings;
    private Integer maxListingsPerLandlord;
    private Integer platformFeePercent;
    private String supportEmail;
    private Boolean notifyOnNewListing;
    private Boolean notifyOnNewVerification;
}
