package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Self-service account fields (authenticated user updates their own row). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 255)
    private String email;

    /** Stored as provided; phone OTP flow expects alignment with Cameroon format when verifying. */
    @Size(max = 32)
    private String phone;
}
