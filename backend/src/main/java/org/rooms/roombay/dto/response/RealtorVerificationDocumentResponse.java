package org.rooms.roombay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rooms.roombay.entity.RealtorVerificationDocument;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtorVerificationDocumentResponse {

    private String id;
    private String documentType;
    private String documentUrl;
    private String status;
    private String rejectionReason;
    private LocalDateTime createdAt;

    public static RealtorVerificationDocumentResponse fromEntity(RealtorVerificationDocument d) {
        return RealtorVerificationDocumentResponse.builder()
                .id(d.getId() == null ? null : d.getId().toString())
                .documentType(d.getDocumentType())
                .documentUrl(d.getDocumentUrl())
                .status(d.getStatus() == null ? null : d.getStatus().name())
                .rejectionReason(d.getRejectionReason())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
