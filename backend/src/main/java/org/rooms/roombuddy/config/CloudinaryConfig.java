package org.rooms.roombay.config;

import com.cloudinary.Cloudinary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class CloudinaryConfig {
    
    @Value("${cloudinary.cloud-name:}")
    private String cloudName;
    
    @Value("${cloudinary.api-key:}")
    private String apiKey;
    
    @Value("${cloudinary.api-secret:}")
    private String apiSecret;
    
    @Bean
    public Cloudinary cloudinary() {
        // Check if Cloudinary credentials are configured
        if (cloudName == null || cloudName.trim().isEmpty() || 
            apiKey == null || apiKey.trim().isEmpty() || 
            apiSecret == null || apiSecret.trim().isEmpty()) {
            
            log.warn("Cloudinary credentials not configured properly. Using mock configuration for development.");
            log.warn("To enable Cloudinary uploads, please set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET environment variables.");
            log.warn("Get free credentials at: https://cloudinary.com/users/register/free");
            
            // Return a mock Cloudinary instance for development
            Map<String, String> mockConfig = new HashMap<>();
            mockConfig.put("cloud_name", "mock-cloud-name");
            mockConfig.put("api_key", "mock-api-key");
            mockConfig.put("api_secret", "mock-api-secret");
            mockConfig.put("secure", "true");
            return new Cloudinary(mockConfig);
        }
        
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName.trim());
        config.put("api_key", apiKey.trim());
        config.put("api_secret", apiSecret.trim());
        config.put("secure", "true");
        
        log.info("Cloudinary configured with cloud name: {}", cloudName);
        return new Cloudinary(config);
    }
}

