package org.rooms.roombay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for sending WhatsApp messages via WhatsApp Business API
 * 
 * For production, integrate with:
 * - WhatsApp Business API (Meta)
 * - Twilio WhatsApp API
 * - MessageBird WhatsApp API
 * 
 * For development/testing, this can be mocked or use a test service
 */
@Service
@Slf4j
public class WhatsAppService {
    
    @Value("${whatsapp.api.enabled:false}")
    private boolean whatsappEnabled;
    
    @Value("${whatsapp.api.url:}")
    private String whatsappApiUrl;
    
    @Value("${whatsapp.api.token:}")
    private String whatsappApiToken;
    
    @Value("${whatsapp.api.phone-number-id:}")
    private String phoneNumberId;
    
    /**
     * Send OTP code via WhatsApp
     * 
     * @param phoneNumber Phone number in format +237XXXXXXXXX
     * @param otpCode 6-digit OTP code
     * @param userName User's first name for personalization
     * @return true if sent successfully, false otherwise
     */
    public boolean sendOtp(String phoneNumber, String otpCode, String userName) {
        log.info("Sending OTP via WhatsApp to: {}", phoneNumber);
        
        if (!whatsappEnabled) {
            log.warn("WhatsApp API is disabled. OTP would be: {} for phone: {}", otpCode, phoneNumber);
            // In development, just log the OTP
            return true; // Return true for development
        }
        
        try {
            // WhatsApp Business API message template
            String message = buildOtpMessage(otpCode, userName);
            
            // TODO: Integrate with actual WhatsApp Business API
            // Example using HTTP client:
            /*
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(whatsappApiToken);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", phoneNumber);
            payload.put("type", "template");
            payload.put("template", Map.of(
                "name", "otp_verification",
                "language", Map.of("code", "en"),
                "components", List.of(
                    Map.of("type", "body",
                           "parameters", List.of(
                               Map.of("type", "text", "text", otpCode)
                           ))
                )
            ));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                whatsappApiUrl + "/" + phoneNumberId + "/messages",
                request,
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
            */
            
            // For now, just log (development mode)
            log.info("WhatsApp OTP message (would send): {}", message);
            return true;
            
        } catch (Exception e) {
            log.error("Error sending WhatsApp OTP to {}: {}", phoneNumber, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Build OTP message for WhatsApp
     */
    private String buildOtpMessage(String otpCode, String userName) {
        return String.format(
            "Hello %s!%n%n" +
            "Your RoomBay verification code is: *%s*%n%n" +
            "This code will expire in 5 minutes.%n%n" +
            "If you didn't request this code, please ignore this message.%n%n" +
            "RoomBay Team",
            userName != null ? userName : "there",
            otpCode
        );
    }
    
    /**
     * Verify if WhatsApp API is configured
     */
    public boolean isConfigured() {
        return whatsappEnabled && 
               whatsappApiUrl != null && !whatsappApiUrl.isEmpty() &&
               whatsappApiToken != null && !whatsappApiToken.isEmpty() &&
               phoneNumberId != null && !phoneNumberId.isEmpty();
    }
}

