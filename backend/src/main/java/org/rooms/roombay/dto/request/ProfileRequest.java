package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @Size(max = 120, message = "City must not exceed 120 characters")
    private String city;

    private String profilePhotoUrl;  // Updated field name to match the actual field

    private String[] languages; // English, French, Pidgin

    @Pattern(regexp = "^\\+237[0-9]{9}$", message = "WhatsApp number must be in format +237XXXXXXXXX")
    private String whatsappNumber;

    @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
    private String emergencyContactName;

    @Pattern(regexp = "^\\+237[0-9]{9}$", message = "Emergency contact phone must be in format +237XXXXXXXXX")
    private String emergencyContactPhone;

    @Size(max = 50, message = "Emergency contact relationship must not exceed 50 characters")
    private String emergencyContactRelationship;

    private String visibility; // PUBLIC, VERIFIED_ONLY, PRIVATE
}

