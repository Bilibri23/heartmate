package org.rooms.roombuddy.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.entity.Lease;
import org.rooms.roombuddy.entity.Notification;
import org.rooms.roombuddy.repository.LeaseRepository;
import org.rooms.roombuddy.service.EmailService;
import org.rooms.roombuddy.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReminderScheduler {

    private final LeaseRepository leaseRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Runs every day at 9:00 AM to send payment reminders
     * Sends reminders 3 days before rent is due (1st of each month)
     */
    @Scheduled(cron = "0 0 9 * * *") // Every day at 9 AM
    public void sendPaymentReminders() {
        log.info("Running payment reminder scheduler...");
        
        LocalDate today = LocalDate.now();
        int dayOfMonth = today.getDayOfMonth();
        
        // Send reminders on the 28th, 29th, 30th, and 1st
        if (dayOfMonth >= 28 || dayOfMonth == 1) {
            sendRentReminders(today);
        }
        
        // Also check for upcoming lease expirations
        checkLeaseExpirations(today);
        
        log.info("Payment reminder scheduler completed.");
    }

    private void sendRentReminders(LocalDate today) {
        List<Lease> activeLeases = leaseRepository.findByStatus(Lease.LeaseStatus.ACTIVE);
        
        int dayOfMonth = today.getDayOfMonth();
        String reminderMessage;
        
        if (dayOfMonth == 1) {
            reminderMessage = "Your rent is due today! Please make your payment to avoid late fees.";
        } else {
            int daysUntilDue = (dayOfMonth >= 28) ? (32 - dayOfMonth) % 31 : 1;
            reminderMessage = "Reminder: Your rent is due in " + daysUntilDue + " day(s). Please prepare your payment.";
        }
        
        for (Lease lease : activeLeases) {
            try {
                // Check if lease is still within its term
                if (lease.getEndDate().isAfter(today)) {
                    // Send in-app notification
                    notificationService.createNotification(
                        lease.getStudent().getId(),
                        Notification.NotificationType.PAYMENT_REMINDER,
                        "Rent Payment Reminder",
                        reminderMessage + " Amount: " + lease.getMonthlyRent() + " XAF for " + lease.getListing().getTitle(),
                        lease.getId(),
                        "LEASE",
                        "/payments?leaseId=" + lease.getId()
                    );
                    
                    // Send email reminder
                    if (lease.getStudent().getEmail() != null) {
                        emailService.sendSimpleEmail(
                            lease.getStudent().getEmail(),
                            "Rent Payment Reminder - RoomBuddy",
                            "Hello " + lease.getStudent().getFirstName() + ",\n\n" +
                            reminderMessage + "\n\n" +
                            "Property: " + lease.getListing().getTitle() + "\n" +
                            "Amount Due: " + lease.getMonthlyRent() + " XAF\n\n" +
                            "Please log in to your RoomBuddy account to make your payment.\n\n" +
                            "Best regards,\nThe RoomBuddy Team"
                        );
                    }
                    
                    log.info("Sent payment reminder to student {} for lease {}", 
                        lease.getStudent().getId(), lease.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send payment reminder for lease {}: {}", lease.getId(), e.getMessage());
            }
        }
    }

    private void checkLeaseExpirations(LocalDate today) {
        List<Lease> activeLeases = leaseRepository.findByStatus(Lease.LeaseStatus.ACTIVE);
        
        for (Lease lease : activeLeases) {
            try {
                long daysUntilExpiry = ChronoUnit.DAYS.between(today, lease.getEndDate());
                
                // Send reminders at 30, 14, 7, and 3 days before expiry
                if (daysUntilExpiry == 30 || daysUntilExpiry == 14 || daysUntilExpiry == 7 || daysUntilExpiry == 3) {
                    String message = "Your lease for " + lease.getListing().getTitle() + 
                        " expires in " + daysUntilExpiry + " days. Please contact your landlord to discuss renewal.";
                    
                    // Notify student
                    notificationService.createNotification(
                        lease.getStudent().getId(),
                        Notification.NotificationType.LEASE_EXPIRING,
                        "Lease Expiring Soon",
                        message,
                        lease.getId(),
                        "LEASE",
                        "/leases"
                    );
                    
                    // Notify landlord
                    notificationService.createNotification(
                        lease.getLandlord().getId(),
                        Notification.NotificationType.LEASE_EXPIRING,
                        "Lease Expiring Soon",
                        "The lease for " + lease.getListing().getTitle() + " with " + 
                            lease.getStudent().getFirstName() + " " + lease.getStudent().getLastName() +
                            " expires in " + daysUntilExpiry + " days.",
                        lease.getId(),
                        "LEASE",
                        "/landlord/leases"
                    );
                    
                    log.info("Sent lease expiration reminder for lease {} ({} days remaining)", 
                        lease.getId(), daysUntilExpiry);
                }
            } catch (Exception e) {
                log.error("Failed to check lease expiration for lease {}: {}", lease.getId(), e.getMessage());
            }
        }
    }
}
