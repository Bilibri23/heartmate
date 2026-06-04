package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.entity.AuditLog;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.repository.AuditLogRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for audit logging.
 * Tracks all important actions for enterprise accountability.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    
    /**
     * Log an admin action (async to not block main flow).
     */
    @Async
    @Transactional
    public void logAdminAction(UUID adminId, String action, String entityType, UUID entityId, String details) {
        try {
            User admin = userRepository.findById(adminId).orElse(null);
            
            AuditLog auditLog = AuditLog.builder()
                    .userId(adminId)
                    .userEmail(admin != null ? admin.getEmail() : null)
                    .userRole("ADMIN")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(AppErrorLogService.sanitizeForOpsLog(details))
                    .status(AuditLog.AuditStatus.SUCCESS)
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} by admin {}", action, adminId);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }
    
    /**
     * Log a user action.
     */
    @Async
    @Transactional
    public void logUserAction(UUID userId, String action, String entityType, UUID entityId, String details) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(user != null ? user.getEmail() : null)
                    .userRole(user != null ? user.getRole().name() : null)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(AppErrorLogService.sanitizeForOpsLog(details))
                    .status(AuditLog.AuditStatus.SUCCESS)
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} by user {}", action, userId);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }
    
    /**
     * Log a system action (no user context).
     */
    @Async
    @Transactional
    public void logSystemAction(String action, String entityType, UUID entityId, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(AppErrorLogService.sanitizeForOpsLog(details))
                    .userRole("SYSTEM")
                    .status(AuditLog.AuditStatus.SUCCESS)
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("System audit log created: {}", action);
        } catch (Exception e) {
            log.error("Failed to create system audit log: {}", e.getMessage());
        }
    }
    
    /**
     * Log a failed action.
     */
    @Async
    @Transactional
    public void logFailedAction(UUID userId, String action, String entityType, UUID entityId, String errorMessage) {
        try {
            User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
            
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(user != null ? user.getEmail() : null)
                    .userRole(user != null ? user.getRole().name() : null)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .status(AuditLog.AuditStatus.FAILURE)
                    .errorMessage(AppErrorLogService.sanitizeForOpsLog(errorMessage))
                    .build();
            
            auditLogRepository.save(auditLog);
            log.warn("Failed action logged: {} - {}", action, errorMessage);
        } catch (Exception e) {
            log.error("Failed to create failed action audit log: {}", e.getMessage());
        }
    }
    
    // ==================== QUERY METHODS ====================
    
    @Transactional(readOnly = true)
    public Page<AuditLog> getAdminActions(Pageable pageable) {
        return auditLogRepository.findAdminActions(pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLog> getActionsByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<AuditLog> getEntityHistory(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLog> getActionsByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditLogRepository.findByDateRange(start, end, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLog> searchActions(String keyword, Pageable pageable) {
        return auditLogRepository.findByActionContainingIgnoreCase(keyword, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentActions(int hours) {
        return auditLogRepository.findRecentActions(LocalDateTime.now().minusHours(hours));
    }
}
