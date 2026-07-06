package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.RealtorProfile;
import org.rooms.roombay.entity.User;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtorProfileResponse {

    private String id;
    private String userId;
    private String fullName;
    private String email;
    private String agencyName;
    private String businessRegistrationNumber;
    private String city;
    private List<String> areasCovered;
    private String phoneNumber;
    private String whatsappNumber;
    private String bio;
    private String verificationStatus;
    private String rejectionReason;
    private Integer trustScore;
    private Integer totalListings;
    private Integer successfulRentals;
    private Integer complaintCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RealtorVerificationDocumentResponse> documents;

    public static RealtorProfileResponse fromEntity(RealtorProfile p, List<RealtorVerificationDocumentResponse> docs) {
        User u = p.getUser();
        String name = u == null ? null
                : ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return RealtorProfileResponse.builder()
                .id(p.getId() == null ? null : p.getId().toString())
                .userId(u == null || u.getId() == null ? null : u.getId().toString())
                .fullName(name)
                .email(u == null ? null : u.getEmail())
                .agencyName(p.getAgencyName())
                .businessRegistrationNumber(p.getBusinessRegistrationNumber())
                .city(p.getCity())
                .areasCovered(splitAreas(p.getAreasCovered()))
                .phoneNumber(p.getPhoneNumber())
                .whatsappNumber(p.getWhatsappNumber())
                .bio(p.getBio())
                .verificationStatus(p.getVerificationStatus() == null ? null : p.getVerificationStatus().name())
                .rejectionReason(p.getRejectionReason())
                .trustScore(p.getTrustScore())
                .totalListings(p.getTotalListings())
                .successfulRentals(p.getSuccessfulRentals())
                .complaintCount(p.getComplaintCount())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .documents(docs)
                .build();
    }

    private static List<String> splitAreas(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
