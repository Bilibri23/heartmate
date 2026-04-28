package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private UUID id;
    private UUID userId;
    private String bio;
    private String city;
    private String profilePhotoUrl;
    private List<String> languages;
    private String whatsappNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

