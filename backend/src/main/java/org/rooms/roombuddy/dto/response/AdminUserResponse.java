package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private UUID id;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private User.Gender gender;
    private LocalDate dateOfBirth;
    private User.UserRole role;
    private User.AccountStatus accountStatus;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean profileCompleted;
    private Boolean suspended;
    private LocalDateTime suspendedAt;
    private UUID suspendedBy;
    private String suspendedByName;
    private String suspensionReason;
    private LocalDateTime lastLoginAt;
    private Integer loginCount;
    private LocalDateTime lastActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
