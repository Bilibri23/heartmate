package org.rooms.roombuddy.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {
    
    private final Cloudinary cloudinary;
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/jpg", "image/png", "image/webp"};
    
    public String uploadImage(MultipartFile file, String folder) {
        log.info("Uploading image to folder: {}", folder);
        
        // Validate file
        validateFile(file);
        
        try {
            // Generate unique filename
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            
            // Upload to Cloudinary
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("folder", folder);
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                uploadParams.put("public_id", filename.substring(0, filename.lastIndexOf('.')));
            } else {
                uploadParams.put("public_id", filename);
            }
            uploadParams.put("overwrite", false);
            uploadParams.put("resource_type", "image");
            
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    uploadParams
            );
            
            String url = (String) uploadResult.get("secure_url");
            if (url == null) {
                log.error("Cloudinary upload succeeded but no URL returned. Result: {}", uploadResult);
                throw new BadRequestException("Upload succeeded but no URL returned. Please try again.");
            }
            
            log.info("Image uploaded successfully: {}", url);
            return url;
            
        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary: {}", e.getMessage(), e);
            String errorMessage = "Failed to upload image";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("api.cloudinary.com") || e.getMessage().contains("Connection")) {
                    errorMessage = "Failed to connect to Cloudinary. Please check your internet connection and Cloudinary configuration.";
                } else {
                    errorMessage = "Failed to upload image: " + e.getMessage();
                }
            }
            throw new BadRequestException(errorMessage);
        } catch (Exception e) {
            log.error("Unexpected error uploading image to Cloudinary: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to upload image: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
    
    public String uploadStudentIdPhoto(MultipartFile file) {
        return uploadImage(file, "student-ids");
    }
    
    public String uploadProfilePhoto(MultipartFile file) {
        return uploadImage(file, "profile-photos");
    }
    
    public String uploadListingPhoto(MultipartFile file) {
        return uploadImage(file, "property-listings");
    }
    
    public void deleteImage(String imageUrl) {
        log.info("Deleting image: {}", imageUrl);
        
        try {
            // Extract public_id from URL
            String publicId = extractPublicIdFromUrl(imageUrl);
            
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Image deleted successfully: {}", publicId);
            }
        } catch (IOException e) {
            log.error("Error deleting image from Cloudinary: {}", e.getMessage());
            // Don't throw exception, just log the error
        }
    }
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed size of 10MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedImageType(contentType)) {
            throw new BadRequestException("Invalid file type. Only JPEG, JPG, PNG, and WEBP are allowed");
        }
    }
    
    private boolean isAllowedImageType(String contentType) {
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equals(contentType)) {
                return true;
            }
        }
        return false;
    }
    
    private String extractPublicIdFromUrl(String url) {
        try {
            // Cloudinary URL format: https://res.cloudinary.com/{cloud_name}/image/upload/{folder}/{public_id}.{format}
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }
            
            String path = url.substring(uploadIndex + 8); // Skip "/upload/"
            int lastDotIndex = path.lastIndexOf('.');
            if (lastDotIndex != -1) {
                path = path.substring(0, lastDotIndex);
            }
            
            return path;
        } catch (Exception e) {
            log.error("Error extracting public_id from URL: {}", url);
            return null;
        }
    }
}

