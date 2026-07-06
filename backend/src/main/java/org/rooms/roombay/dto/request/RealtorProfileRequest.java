package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Create/update a realtor profile (onboarding form). Used by POST /api/realtors/register and PUT /api/realtors/me. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtorProfileRequest {

    @NotBlank(message = "Agency or agent name is required")
    private String agencyName;

    private String businessRegistrationNumber;

    @NotBlank(message = "City is required")
    private String city;

    /** Neighborhoods/areas the realtor covers. */
    private List<String> areasCovered;

    private String phoneNumber;

    private String whatsappNumber;

    private String bio;
}
