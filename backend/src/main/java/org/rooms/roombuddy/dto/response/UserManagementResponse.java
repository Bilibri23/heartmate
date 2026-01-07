package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombuddy.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementResponse {
    private UUID id;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String fullName;
    private User.Gender gender;
    private LocalDate dateOfBirth;
    private User.UserRole role;
    private User.AccountStatus accountStatus;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean profileCompleted;
    private LocalDateTime lastActive;
    private LocalDateTime suspendedAt;
    private UUID suspendedBy;
    private String suspensionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional computed fields
    private Long listingsCount;
    private Long verificationsCount;
    private Boolean isSuspended;
}
