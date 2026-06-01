package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.ApiResponse;
import org.rooms.roombay.service.FileUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload", description = "APIs for file uploads")
public class FileUploadController {
    
    private final FileUploadService fileUploadService;
    
    @PostMapping("/profile-photo")
    @Operation(summary = "Upload profile photo", description = "Upload a profile photo (max 10MB)")
    public ResponseEntity<ApiResponse<String>> uploadProfilePhoto(@RequestParam("file") MultipartFile file) {
        log.info("Uploading profile photo");
        String url = fileUploadService.uploadProfilePhoto(file);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Profile photo uploaded successfully")
                .data(url)
                .build());
    }
    
    @PostMapping("/student-id")
    @Operation(summary = "Upload student ID photo", description = "Upload a student ID photo (max 10MB)")
    public ResponseEntity<ApiResponse<String>> uploadStudentIdPhoto(@RequestParam("file") MultipartFile file) {
        log.info("Uploading student ID photo");
        String url = fileUploadService.uploadStudentIdPhoto(file);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Student ID photo uploaded successfully")
                .data(url)
                .build());
    }

    @PostMapping("/verification-document")
    @Operation(summary = "Upload verification document", description = "Upload ID or selfie for tenant verification (max 10MB)")
    public ResponseEntity<ApiResponse<String>> uploadVerificationDocument(@RequestParam("file") MultipartFile file) {
        log.info("Uploading verification document");
        String url = fileUploadService.uploadVerificationDocument(file);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Verification document uploaded successfully")
                .data(url)
                .build());
    }
    
    @DeleteMapping("/image")
    @Operation(summary = "Delete image", description = "Delete an image from Cloudinary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@RequestParam("url") String imageUrl) {
        log.info("Deleting image: {}", imageUrl);
        fileUploadService.deleteImage(imageUrl);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Image deleted successfully")
                .build());
    }
}
