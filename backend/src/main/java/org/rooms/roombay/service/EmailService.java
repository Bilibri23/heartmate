package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;

    /** Optional; otherwise uses MAIL_USERNAME */
    @Value("${app.mail.from:}")
    private String mailFromOverride;

    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    private void applyFrom(SimpleMailMessage message) {
        String from = StringUtils.hasText(mailFromOverride) ? mailFromOverride.trim()
                : (StringUtils.hasText(fromEmail) ? fromEmail.trim() : null);
        if (from != null) {
            message.setFrom(from);
        } else {
            log.warn("Email From is not set (configure MAIL_USERNAME or app.mail.from); SMTP may reject or fail to parse headers");
        }
    }
    
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        log.info("Sending password reset email to: {}", toEmail);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            applyFrom(message);
            message.setTo(toEmail);
            message.setSubject("Password Reset Request - RoomBay");
            message.setText(buildPasswordResetEmailBody(resetToken));
            
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Error sending password reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
        }
    }

    public void sendEmailVerificationEmail(String toEmail, String verificationToken) {
        log.info("Sending email verification email to: {}", toEmail);
        sendEmail(toEmail, "Verify Your Email - RoomBay", buildEmailVerificationBody(verificationToken));
    }
    
    public void sendVerificationStatusEmail(String toEmail, String status, String reason) {
        log.info("Sending verification status email to: {}", toEmail);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            applyFrom(message);
            message.setTo(toEmail);
            message.setSubject("Tenant Verification Status - RoomBay");
            message.setText(buildVerificationStatusEmailBody(status, reason));
            
            mailSender.send(message);
            log.info("Verification status email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Error sending verification status email to {}: {}", toEmail, e.getMessage());
            // Don't throw exception, just log the error
        }
    }
    
    private String buildPasswordResetEmailBody(String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        
        return "Hello,\n\n" +
                "You have requested to reset your password for your RoomBay account.\n\n" +
                "Please click on the following link to reset your password:\n" +
                resetLink + "\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "RoomBay Team";
    }

    private String buildEmailVerificationBody(String token) {
        String verificationLink = baseUrl + "/api/auth/verify-email?token=" + token;
        return "Hello,\n\n" +
                "Welcome to RoomBay! Please verify your email address to secure your account.\n\n" +
                "Click the link below to verify your email:\n" +
                verificationLink + "\n\n" +
                "This link expires in 24 hours.\n\n" +
                "If you did not create this account, you can ignore this email.\n\n" +
                "Best regards,\n" +
                "RoomBay Team";
    }
    
    private String buildVerificationStatusEmailBody(String status, String reason) {
        String body = "Hello,\n\n" +
                "Your student verification status has been updated.\n\n" +
                "Status: " + status + "\n\n";
        
        if (status.equals("REJECTED") && reason != null) {
            body += "Reason: " + reason + "\n\n";
        }
        
        if (status.equals("VERIFIED")) {
            body += "Congratulations! Your student verification has been approved. " +
                    "You now have access to verified student features.\n\n";
        }
        
        body += "Best regards,\n" +
                "RoomBay Team";
        
        return body;
    }
    
    public void sendMatchAcceptedEmail(String toEmail, String matchedUserName) {
        log.info("Sending match accepted email to: {}", toEmail);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            applyFrom(message);
            message.setTo(toEmail);
            message.setSubject("New Roommate Match - RoomBay");
            message.setText(buildMatchAcceptedEmailBody(matchedUserName));
            
            mailSender.send(message);
            log.info("Match accepted email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Error sending match accepted email to {}: {}", toEmail, e.getMessage());
            // Don't throw exception, just log the error
        }
    }
    
    private String buildMatchAcceptedEmailBody(String matchedUserName) {
        return "Hello,\n\n" +
                "Great news! You have a new mutual match on RoomBay!\n\n" +
                matchedUserName + " has also accepted your match request. " +
                "You can now start messaging and coordinating with your potential roommate.\n\n" +
                "Log in to your RoomBay account to start the conversation!\n\n" +
                "Best regards,\n" +
                "RoomBay Team";
    }
    
    // Application notifications
    public void sendApplicationReceivedEmail(String toEmail, String studentName, String listingTitle) {
        sendEmail(toEmail, "New Application Received - RoomBay",
            "Hello,\n\n" +
            "You have received a new application!\n\n" +
            "Student: " + studentName + "\n" +
            "Property: " + listingTitle + "\n\n" +
            "Log in to your RoomBay account to review the application.\n\n" +
            "Best regards,\nRoomBay Team");
    }
    
    public void sendApplicationStatusEmail(String toEmail, String listingTitle, String status) {
        String statusMessage = status.equals("ACCEPTED") 
            ? "Congratulations! Your application has been accepted."
            : "Unfortunately, your application was not accepted at this time.";
            
        sendEmail(toEmail, "Application Update - RoomBay",
            "Hello,\n\n" +
            "Your application for \"" + listingTitle + "\" has been updated.\n\n" +
            "Status: " + status + "\n\n" +
            statusMessage + "\n\n" +
            "Log in to your RoomBay account for more details.\n\n" +
            "Best regards,\nRoomBay Team");
    }
    
    // Payment notifications
    public void sendPaymentVerifiedEmail(String toEmail, int amount, String listingTitle) {
        sendEmail(toEmail, "Payment Verified - RoomBay",
            "Hello,\n\n" +
            "Great news! Your payment has been verified.\n\n" +
            "Amount: " + amount + " XAF\n" +
            "Property: " + listingTitle + "\n\n" +
            "Your lease is now active. Welcome to your new home!\n\n" +
            "Best regards,\nRoomBay Team");
    }
    
    public void sendPaymentReceivedEmail(String toEmail, int amount, String studentName, String listingTitle) {
        sendEmail(toEmail, "Payment Received - RoomBay",
            "Hello,\n\n" +
            "You have received a payment!\n\n" +
            "Amount: " + amount + " XAF\n" +
            "From: " + studentName + "\n" +
            "Property: " + listingTitle + "\n\n" +
            "The payment has been verified and will be transferred to your account.\n\n" +
            "Best regards,\nRoomBay Team");
    }
    
    // Lease notifications
    public void sendLeaseCreatedEmail(String toEmail, String listingTitle) {
        sendEmail(toEmail, "Lease Created - RoomBay",
            "Hello,\n\n" +
            "A lease has been created for \"" + listingTitle + "\".\n\n" +
            "Please log in to review the lease terms and complete the payment.\n\n" +
            "Best regards,\nRoomBay Team");
    }
    
    // Welcome email
    public void sendWelcomeEmail(String toEmail, String firstName) {
        sendEmail(toEmail, "Welcome to RoomBay!",
            "Hello " + firstName + ",\n\n" +
            "Welcome to RoomBay! We're excited to have you on board.\n\n" +
            "RoomBay helps students find their perfect accommodation and connect with compatible roommates.\n\n" +
            "Get started by:\n" +
            "1. Completing your profile\n" +
            "2. Setting your preferences\n" +
            "3. Browsing available listings\n\n" +
            "If you have any questions, feel free to reach out to our support team.\n\n" +
            "Best regards,\nThe RoomBay Team");
    }
    
    private void sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            applyFrom(message);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to: {} with subject: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", toEmail, e.getMessage());
        }
    }
    
    /**
     * Send a simple email with custom subject and message.
     * Used for landlord verification notifications and other custom emails.
     */
    public void sendSimpleEmail(String toEmail, String subject, String message) {
        sendEmail(toEmail, subject, message);
    }
    
    /**
     * Send landlord verification status email.
     */
    public void sendLandlordVerificationEmail(String toEmail, String verificationType, String status, String reason) {
        log.info("Sending landlord {} verification status email to: {}", verificationType, toEmail);
        
        String subject = status.equals("VERIFIED") 
            ? "Your " + verificationType + " Verification is Approved - RoomBay"
            : "Your " + verificationType + " Verification Needs Attention - RoomBay";
        
        StringBuilder body = new StringBuilder();
        body.append("Hello,\n\n");
        body.append("Your ").append(verificationType.toLowerCase()).append(" verification status has been updated.\n\n");
        body.append("Status: ").append(status).append("\n\n");
        
        if (status.equals("VERIFIED")) {
            body.append("Congratulations! Your ").append(verificationType.toLowerCase())
                .append(" has been verified. You now have a verified badge on your listings, ")
                .append("which helps build trust with potential tenants.\n\n");
        } else if (status.equals("REJECTED") && reason != null) {
            body.append("Reason: ").append(reason).append("\n\n");
            body.append("Please review the feedback and resubmit your verification documents.\n\n");
        }
        
        body.append("Best regards,\nThe RoomBay Team");
        
        sendEmail(toEmail, subject, body.toString());
    }
}

