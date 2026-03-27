package org.rooms.roombay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {
    
    private String university;
    
    private String studentId;
    
    private String faculty;
    
    private String department;
    
    @Min(value = 1, message = "Year of study must be at least 1")
    @Max(value = 10, message = "Year of study must be at most 10")
    private Integer yearOfStudy;
    
    private String studentIdPhotoUrl;

    private String idType;

    private String idNumber;

    private String idPhotoUrl;

    private String selfiePhotoUrl;
}

