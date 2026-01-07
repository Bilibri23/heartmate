package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.User;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    private String firstName;
    private String lastName;
    private User.Gender gender;
    private LocalDate dateOfBirth;
    
    @NotNull(message = "Role is required")
    private User.UserRole role;
    
    @NotNull(message = "Account status is required")
    private User.AccountStatus accountStatus;
}
