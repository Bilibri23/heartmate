package org.rooms.roombay.repository;

import org.rooms.roombay.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    // Find by user
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);
    
    // Find by action type
    Page<AuditLog> findByAction(String action, Pageable pageable);
    
    // Find by entity
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);
    
    // Find by date range
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<AuditLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate, 
            Pageable pageable);
    
    // Find admin actions
    @Query("SELECT a FROM AuditLog a WHERE a.userRole = 'ADMIN' ORDER BY a.createdAt DESC")
    Page<AuditLog> findAdminActions(Pageable pageable);
    
    // Search by action containing keyword
    Page<AuditLog> findByActionContainingIgnoreCase(String keyword, Pageable pageable);
    
    // Count by action type
    long countByAction(String action);
    
    // Find recent actions
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentActions(@Param("since") LocalDateTime since);
}
