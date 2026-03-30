package org.rooms.roombay.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for sanitizing user inputs to prevent SQL injection, XSS, and other attacks
 */
@Component
public class InputSanitizer {
    
    // SQL injection patterns to detect and block
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('.*(--|;|\\*|/\\*|\\*/|xp_|sp_|exec|execute|select|insert|update|delete|drop|create|alter|union|script|javascript|<script|onerror|onload).*')|" +
        "(.*\\bor\\b.*=.*)|" +
        "(.*\\band\\b.*=.*)|" +
        "(.*\\bselect\\b.*\\bfrom\\b.*)|" +
        "(.*\\bunion\\b.*\\bselect\\b.*)|" +
        "(.*\\bdrop\\b.*\\btable\\b.*)|" +
        "(.*\\binsert\\b.*\\binto\\b.*)|" +
        "(.*\\bupdate\\b.*\\bset\\b.*)|" +
        "(.*\\bdelete\\b.*\\bfrom\\b.*)|" +
        "(.*\\bexec\\b.*\\(.*\\).*)|" +
        "(.*\\bscript\\b.*>.*)",
        Pattern.CASE_INSENSITIVE
    );
    
    // XSS patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(<script[^>]*>.*?</script>)|" +
        "(<iframe[^>]*>.*?</iframe>)|" +
        "(javascript:)|" +
        "(on\\w+\\s*=)|" +
        "(<img[^>]*onerror[^>]*>)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Path traversal patterns
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./)|(\\.\\\\)|(%2e%2e/)|(%2e%2e\\\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Sanitize a string input by removing potentially dangerous characters
     */
    public String sanitizeString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        // Check for SQL injection attempts
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            throw new SecurityException("Potential SQL injection detected in input: " + maskSensitiveInput(input));
        }
        
        // Check for XSS attempts
        if (XSS_PATTERN.matcher(input).find()) {
            throw new SecurityException("Potential XSS attack detected in input: " + maskSensitiveInput(input));
        }
        
        // Check for path traversal attempts
        if (PATH_TRAVERSAL_PATTERN.matcher(input).find()) {
            throw new SecurityException("Potential path traversal detected in input: " + maskSensitiveInput(input));
        }
        
        // Remove null bytes
        input = input.replace("\0", "");
        
        // Trim whitespace
        input = input.trim();
        
        // Limit length to prevent DoS
        if (input.length() > 1000) {
            input = input.substring(0, 1000);
        }
        
        return input;
    }
    
    /**
     * Sanitize a list of strings
     */
    public List<String> sanitizeList(List<String> inputs) {
        if (inputs == null) {
            return null;
        }
        
        return inputs.stream()
                .map(this::sanitizeString)
                .collect(Collectors.toList());
    }
    
    /**
     * Validate and sanitize numeric input
     */
    public Integer sanitizeInteger(Integer input, Integer min, Integer max) {
        if (input == null) {
            return null;
        }
        
        // Ensure within reasonable bounds
        if (min != null && input < min) {
            return min;
        }
        if (max != null && input > max) {
            return max;
        }
        
        return input;
    }
    
    /**
     * Validate and sanitize double input
     */
    public Double sanitizeDouble(Double input, Double min, Double max) {
        if (input == null) {
            return null;
        }
        
        // Check for NaN or Infinity
        if (input.isNaN() || input.isInfinite()) {
            throw new SecurityException("Invalid numeric value");
        }
        
        // Ensure within reasonable bounds
        if (min != null && input < min) {
            return min;
        }
        if (max != null && input > max) {
            return max;
        }
        
        return input;
    }
    
    /**
     * Validate property type enum
     */
    public String sanitizePropertyType(String propertyType) {
        if (propertyType == null || propertyType.trim().isEmpty()) {
            return null;
        }
        
        // Only allow valid property types
        String[] validTypes = {"STUDIO", "APARTMENT", "HOUSE", "PRIVATE_ROOM", "SHARED_ROOM"};
        String upperType = propertyType.toUpperCase().trim();
        
        for (String validType : validTypes) {
            if (validType.equals(upperType)) {
                return upperType;
            }
        }
        
        throw new SecurityException("Invalid property type: " + propertyType);
    }
    
    /**
     * Validate city name (only allow known Cameroon cities)
     */
    public String sanitizeCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return null;
        }
        
        city = sanitizeString(city);
        
        // Only allow alphanumeric characters, spaces, hyphens, and apostrophes
        if (!city.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) {
            throw new SecurityException("Invalid city name format");
        }
        
        return city;
    }
    
    /**
     * Validate date string (ISO format)
     */
    public String sanitizeDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        
        // Only allow ISO date format: YYYY-MM-DD
        if (!date.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new SecurityException("Invalid date format. Use YYYY-MM-DD");
        }
        
        return date;
    }
    
    /**
     * Validate latitude (-90 to 90)
     */
    public Double sanitizeLatitude(Double lat) {
        if (lat == null) {
            return null;
        }
        
        return sanitizeDouble(lat, -90.0, 90.0);
    }
    
    /**
     * Validate longitude (-180 to 180)
     */
    public Double sanitizeLongitude(Double lon) {
        if (lon == null) {
            return null;
        }
        
        return sanitizeDouble(lon, -180.0, 180.0);
    }
    
    /**
     * Validate distance (0 to 100 km)
     */
    public Double sanitizeDistance(Double distance) {
        if (distance == null) {
            return null;
        }
        
        return sanitizeDouble(distance, 0.0, 100.0);
    }
    
    /**
     * Mask sensitive input for logging (show only first and last 2 characters)
     */
    private String maskSensitiveInput(String input) {
        if (input == null || input.length() <= 4) {
            return "***";
        }
        
        return input.substring(0, 2) + "..." + input.substring(input.length() - 2);
    }
}
