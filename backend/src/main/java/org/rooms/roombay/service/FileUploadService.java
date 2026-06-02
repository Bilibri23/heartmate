package org.rooms.roombay.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {
    
    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

    @Value("${cloudinary.api-key:}")
    private String cloudinaryApiKey;

    @Value("${cloudinary.api-secret:}")
    private String cloudinaryApiSecret;
    
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB for profile and verification images
    private static final long MAX_LISTING_IMAGE_SIZE = 100 * 1024 * 1024; // 100MB for 360-degree panoramic listing images
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    private static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/jpg", "image/png", "image/webp"};
    private static final String[] ALLOWED_VIDEO_TYPES = {"video/mp4", "video/quicktime", "video/x-msvideo", "video/webm"};
    
    public String uploadImage(MultipartFile file, String folder) {
        return uploadImage(file, folder, MAX_IMAGE_SIZE);
    }

    private String uploadImage(MultipartFile file, String folder, long maxBytes) {
        log.info("Uploading image to folder: {}", folder);
        
        // Validate file
        validateFile(file, maxBytes);
        
        try {
            // Check if we're using mock Cloudinary
            if (isMockCloudinary()) {
                log.warn("Using mock Cloudinary - returning simulated URL for development");
                return generateMockImageUrl(file, folder);
            }
            
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
            if (isLikelyCloudinaryConnectivityFailure(e)) {
                throw new BadRequestException(cloudinaryConnectivityMessage());
            }
            throw new BadRequestException("Failed to upload image: " + (e.getMessage() != null ? e.getMessage() : "I/O error"));
        } catch (Exception e) {
            log.error("Unexpected error uploading image to Cloudinary: {}", e.getMessage(), e);
            if (isLikelyCloudinaryConnectivityFailure(e)) {
                throw new BadRequestException(cloudinaryConnectivityMessage());
            }
            throw new BadRequestException("Failed to upload image: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
    
    public String uploadStudentIdPhoto(MultipartFile file) {
        return uploadImage(file, "student-ids");
    }

    public String uploadVerificationDocument(MultipartFile file) {
        return uploadImage(file, "tenant-verifications");
    }
    
    public String uploadProfilePhoto(MultipartFile file) {
        return uploadImage(file, "profile-photos");
    }
    
    public String uploadListingPhoto(MultipartFile file) {
        return uploadImage(file, "property-listings", MAX_LISTING_IMAGE_SIZE);
    }
    
    public String uploadVideoTour(MultipartFile file) {
        return uploadVideo(file, "video-tours");
    }
    
    public String uploadVideo(MultipartFile file, String folder) {
        log.info("Uploading video to folder: {}", folder);
        
        validateVideoFile(file);
        
        try {
            if (isMockCloudinary()) {
                log.warn("Using mock Cloudinary - returning simulated video URL for development");
                return String.format("https://picsum.photos/800/450?random=%s", UUID.randomUUID().toString());
            }
            
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("folder", folder);
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                uploadParams.put("public_id", filename.substring(0, filename.lastIndexOf('.')));
            } else {
                uploadParams.put("public_id", filename);
            }
            uploadParams.put("resource_type", "video");
            
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    uploadParams
            );
            
            String url = (String) uploadResult.get("secure_url");
            if (url == null) {
                throw new BadRequestException("Video upload succeeded but no URL returned.");
            }
            
            log.info("Video uploaded successfully: {}", url);
            return url;
            
        } catch (IOException e) {
            log.error("Error uploading video: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to upload video: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error uploading video: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to upload video: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
    
    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Video file is required");
        }
        
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new BadRequestException("Video file size exceeds maximum allowed size of 100MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedVideoType(contentType)) {
            throw new BadRequestException("Invalid video type. Only MP4, MOV, AVI, and WEBM are allowed");
        }
        if (!matchesDeclaredVideoType(file, contentType)) {
            throw new BadRequestException("Video content does not match the declared file type");
        }
    }
    
    private boolean isAllowedVideoType(String contentType) {
        for (String allowedType : ALLOWED_VIDEO_TYPES) {
            if (allowedType.equals(contentType)) {
                return true;
            }
        }
        return false;
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
    
    private void validateFile(MultipartFile file, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("File size exceeds maximum allowed size of " + (maxBytes / (1024 * 1024)) + "MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedImageType(contentType)) {
            throw new BadRequestException("Invalid file type. Only JPEG, JPG, PNG, and WEBP are allowed");
        }
        if (!matchesDeclaredImageType(file, contentType)) {
            throw new BadRequestException("File content does not match the declared image type");
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

    private static boolean matchesDeclaredImageType(MultipartFile file, String contentType) {
        byte[] header = readHeader(file, 16);
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/webp" -> asciiAt(header, 0, "RIFF") && asciiAt(header, 8, "WEBP");
            default -> false;
        };
    }

    private static boolean matchesDeclaredVideoType(MultipartFile file, String contentType) {
        byte[] header = readHeader(file, 16);
        return switch (contentType) {
            case "video/mp4", "video/quicktime" -> asciiAt(header, 4, "ftyp");
            case "video/webm" -> startsWith(header, 0x1A, 0x45, 0xDF, 0xA3);
            case "video/x-msvideo" -> asciiAt(header, 0, "RIFF") && asciiAt(header, 8, "AVI ");
            default -> false;
        };
    }

    private static byte[] readHeader(MultipartFile file, int maxBytes) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(maxBytes);
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded file");
        }
    }

    private static boolean startsWith(byte[] actual, int... expected) {
        if (actual.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((actual[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean asciiAt(byte[] actual, int offset, String expected) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        if (actual.length < offset + expectedBytes.length) {
            return false;
        }
        for (int i = 0; i < expectedBytes.length; i++) {
            if (actual[offset + i] != expectedBytes[i]) {
                return false;
            }
        }
        return true;
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
    
    private boolean isMockCloudinary() {
        return cloudinaryCloudName == null || cloudinaryCloudName.trim().isEmpty() ||
               cloudinaryApiKey == null || cloudinaryApiKey.trim().isEmpty() ||
               cloudinaryApiSecret == null || cloudinaryApiSecret.trim().isEmpty() ||
               "mock-cloud-name".equals(cloudinaryCloudName) ||
               "mock-api-key".equals(cloudinaryApiKey);
    }
    
    private String generateMockImageUrl(MultipartFile file, String folder) {
        // Generate a realistic mock URL for development
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        if (filename.contains(".")) {
            filename = filename.substring(0, filename.lastIndexOf("."));
        }
        
        // Use a placeholder image service or local mock URL
        return String.format("https://picsum.photos/400/300?random=%s", UUID.randomUUID().toString());
    }

    private static boolean isLikelyCloudinaryConnectivityFailure(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return true;
            }
            String m = t.getMessage();
            if (m != null) {
                String lower = m.toLowerCase();
                if (lower.contains("unknown host")
                        || lower.contains("no such host")
                        || lower.contains("api.cloudinary.com")
                        || lower.contains("nodename nor servname")
                        || lower.contains("failed to connect")
                        || lower.contains("connection refused")
                        || lower.contains("connection timed out")
                        || lower.contains("connect timed out")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private static String cloudinaryConnectivityMessage() {
        return "Could not reach Cloudinary (image host). Check internet/DNS or firewall. "
                + "For local dev without outbound access, leave cloudinary credentials unset to use mock image URLs.";
    }
}
