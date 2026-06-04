package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSettingsUpdateRequest {

    @NotNull
    private Boolean maintenanceMode;

    @NotNull
    private Boolean allowNewRegistrations;

    @NotNull
    private Boolean requireEmailVerification;

    @NotNull
    private Boolean requireTenantVerification;

    @NotNull
    private Boolean autoApproveListings;

    @NotNull
    @Min(1)
    @Max(1000)
    private Integer maxListingsPerLandlord;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer platformFeePercent;

    @NotNull
    @Email
    private String supportEmail;

    @NotNull
    private Boolean notifyOnNewListing;

    @NotNull
    private Boolean notifyOnNewVerification;
}
