package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.User;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthMeResponse {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean emailVerified;
    private Boolean phoneVerified;

    public static AuthMeResponse from(User u) {
        return AuthMeResponse.builder()
                .userId(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(u.getRole() != null ? u.getRole().name() : null)
                .emailVerified(u.getEmailVerified())
                .phoneVerified(u.getPhoneVerified())
                .build();
    }
}
