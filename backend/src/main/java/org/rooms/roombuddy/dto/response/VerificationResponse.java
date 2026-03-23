package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String university;
    private String studentId;
    private String faculty;
    private String department;
    private Integer yearOfStudy;
    private String studentIdPhotoUrl;
    private String idType;
    private String idNumber;
    private String idPhotoUrl;
    private String selfiePhotoUrl;
    private String status;
    private String rejectionReason;
    private UUID verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

