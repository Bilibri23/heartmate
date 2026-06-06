package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.NotificationResponse;
import org.rooms.roombay.entity.Notification;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.NotificationRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    
    @Transactional
    public void createNotification(UUID userId, Notification.NotificationType type, 
                                   String title, String message, 
                                   UUID referenceId, String referenceType, String actionUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .actionUrl(actionUrl)
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Created notification for user {}: {}", userId, type);
        
        // Send real-time notification via WebSocket
        try {
            NotificationResponse response = mapToResponse(savedNotification);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    response
            );
            log.info("Sent real-time notification to user {} via WebSocket", userId);
        } catch (Exception e) {
            log.error("Failed to send real-time notification via WebSocket: {}", e.getMessage());
            // Don't fail the notification creation if WebSocket fails
        }
    }
    
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
        } else {
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return notifications.map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        
        return mapToResponse(notification);
    }
    
    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }
    
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        
        notificationRepository.delete(notification);
    }
    
    // Helper methods for creating specific notification types with email
    public void notifyApplicationReceived(UUID landlordId, UUID applicationId, String studentName, String listingTitle) {
        createNotification(landlordId, Notification.NotificationType.APPLICATION_RECEIVED,
                "New Application Received",
                studentName + " has applied for " + listingTitle,
                applicationId, "APPLICATION", "/landlord/applications");
        
        // Send email
        User landlord = userRepository.findById(landlordId).orElse(null);
        if (landlord != null && landlord.getEmail() != null) {
            emailService.sendApplicationReceivedEmail(landlord.getEmail(), studentName, listingTitle);
        }
    }
    
    public void notifyApplicationAccepted(UUID studentId, UUID applicationId, String listingTitle) {
        createNotification(studentId, Notification.NotificationType.APPLICATION_ACCEPTED,
                "Application Accepted!",
                "Your application for " + listingTitle + " has been accepted",
                applicationId, "APPLICATION", "/applications");
        
        // Send email
        User student = userRepository.findById(studentId).orElse(null);
        if (student != null && student.getEmail() != null) {
            emailService.sendApplicationStatusEmail(student.getEmail(), listingTitle, "ACCEPTED");
        }
    }
    
    public void notifyApplicationRejected(UUID studentId, UUID applicationId, String listingTitle) {
        createNotification(studentId, Notification.NotificationType.APPLICATION_REJECTED,
                "Application Update",
                "Your application for " + listingTitle + " was not accepted",
                applicationId, "APPLICATION", "/applications");
        
        // Send email
        User student = userRepository.findById(studentId).orElse(null);
        if (student != null && student.getEmail() != null) {
            emailService.sendApplicationStatusEmail(student.getEmail(), listingTitle, "REJECTED");
        }
    }
    
    public void notifyLeaseCreated(UUID studentId, UUID leaseId, String listingTitle) {
        createNotification(studentId, Notification.NotificationType.LEASE_CREATED,
                "Lease Created",
                "A lease has been created for " + listingTitle + ". Please review and sign.",
                leaseId, "LEASE", "/leases");
        
        // Send email
        User student = userRepository.findById(studentId).orElse(null);
        if (student != null && student.getEmail() != null) {
            emailService.sendLeaseCreatedEmail(student.getEmail(), listingTitle);
        }
    }
    
    public void notifyPaymentVerified(UUID studentId, UUID paymentId, String amount) {
        createNotification(studentId, Notification.NotificationType.PAYMENT_VERIFIED,
                "Payment Verified",
                "Your payment of " + amount + " XAF has been verified",
                paymentId, "PAYMENT", "/leases");
        
        // Send email - amount is string, parse it
        User student = userRepository.findById(studentId).orElse(null);
        if (student != null && student.getEmail() != null) {
            try {
                int amountInt = Integer.parseInt(amount.replaceAll("[^0-9]", ""));
                emailService.sendPaymentVerifiedEmail(student.getEmail(), amountInt, "Your property");
            } catch (Exception e) {
                log.warn("Could not send payment verified email: {}", e.getMessage());
            }
        }
    }
    
    public void notifyPaymentRejected(UUID studentId, UUID paymentId, String reason) {
        createNotification(studentId, Notification.NotificationType.PAYMENT_REJECTED,
                "Payment Rejected",
                "Your payment was rejected: " + reason,
                paymentId, "PAYMENT", "/payments");
    }
    
    public void notifyPaymentReceived(UUID landlordId, UUID paymentId, String amount, String studentName) {
        createNotification(landlordId, Notification.NotificationType.PAYMENT_RECEIVED,
                "Payment Received",
                "You received " + amount + " XAF from " + studentName,
                paymentId, "PAYMENT", "/landlord/payments");
        
        // Send email
        User landlord = userRepository.findById(landlordId).orElse(null);
        if (landlord != null && landlord.getEmail() != null) {
            try {
                int amountInt = Integer.parseInt(amount.replaceAll("[^0-9]", ""));
                emailService.sendPaymentReceivedEmail(landlord.getEmail(), amountInt, studentName, "Your property");
            } catch (Exception e) {
                log.warn("Could not send payment received email: {}", e.getMessage());
            }
        }
    }
    
    public void notifyNewMessage(UUID userId, UUID conversationId, String senderName) {
        createNotification(userId, Notification.NotificationType.NEW_MESSAGE,
                "New Message",
                "You have a new message from " + senderName,
                conversationId, "MESSAGE", "/messages");
    }
    
    public void notifyReviewReceived(UUID userId, UUID reviewId, String reviewerName) {
        createNotification(userId, Notification.NotificationType.REVIEW_RECEIVED,
                "New Review",
                reviewerName + " left you a review",
                reviewId, "REVIEW", "/reviews");
    }

    public void notifyStudentVerificationSubmitted(UUID verificationId, String userName) {
        notifyAdmins(
                Notification.NotificationType.VERIFICATION_SUBMITTED,
                "New tenant verification",
                userName + " submitted documents for review.",
                verificationId,
                "VERIFICATION",
                "/admin/verifications"
        );
    }

    public void notifyLandlordVerificationSubmitted(UUID verificationId, String userName, String verificationType) {
        notifyAdmins(
                Notification.NotificationType.VERIFICATION_SUBMITTED,
                "New landlord verification",
                userName + " submitted " + verificationType.toLowerCase() + " documents for review.",
                verificationId,
                "LANDLORD_VERIFICATION",
                "/admin/verifications"
        );
    }

    private void notifyAdmins(Notification.NotificationType type, String title, String message,
                              UUID referenceId, String referenceType, String actionUrl) {
        userRepository.findAllByRole(User.UserRole.ADMIN).forEach(admin -> {
            try {
                if (notificationRepository.existsByUserIdAndTypeAndReferenceIdAndReferenceTypeAndReadFalse(
                        admin.getId(), type, referenceId, referenceType)) {
                    log.debug("Skipping duplicate unread admin notification user={} type={} referenceType={} referenceId={}",
                            admin.getId(), type, referenceType, referenceId);
                    return;
                }
                createNotification(admin.getId(), type, title, message, referenceId, referenceType, actionUrl);
            } catch (Exception e) {
                log.error("Failed to notify admin {} about {}: {}", admin.getId(), referenceType, e.getMessage());
            }
        });
    }
    
    public void notifyDisputeUpdated(UUID userId, UUID disputeId, String status) {
        createNotification(userId, Notification.NotificationType.DISPUTE_UPDATED,
                "Dispute Update",
                "Your dispute status has been updated to: " + status,
                disputeId, "DISPUTE", "/admin/my-disputes");
    }
    
    public void notifyDisputeResolved(UUID userId, UUID disputeId, String outcome) {
        createNotification(userId, Notification.NotificationType.DISPUTE_RESOLVED,
                "Dispute Resolved",
                "Your dispute has been resolved: " + outcome,
                disputeId, "DISPUTE", "/admin/my-disputes");
    }
    
    public void notifyNewMatch(UUID userId, UUID matchId, String matchedUserName, int compatibilityScore) {
        createNotification(userId, Notification.NotificationType.NEW_MATCH,
                "New Match Found!",
                "You matched with " + matchedUserName + " (" + compatibilityScore + "% compatibility)",
                matchId, "MATCH", "/admin/matches");
    }
    
    public void notifyMutualMatch(UUID userId, UUID matchId, String matchedUserName) {
        createNotification(userId, Notification.NotificationType.MUTUAL_MATCH,
                "It's a Match! 🎉",
                matchedUserName + " also liked you back! You can now chat.",
                matchId, "MATCH", "/admin/matches");
    }
    
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .actionUrl(notification.getActionUrl())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
