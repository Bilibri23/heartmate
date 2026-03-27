package org.rooms.roombay.repository;

import org.rooms.roombay.entity.Report;
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
public interface ReportRepository extends JpaRepository<Report, UUID> {
    
    // Find reports by status
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);
    
    // Find reports by entity type
    Page<Report> findByReportedEntityType(Report.EntityType entityType, Pageable pageable);
    
    // Find reports by entity
    List<Report> findByReportedEntityTypeAndReportedEntityId(
        Report.EntityType entityType, 
        UUID entityId
    );
    
    // Find reports by reporter
    Page<Report> findByReporterId(UUID reporterId, Pageable pageable);
    
    // Find reports by priority
    Page<Report> findByPriority(Report.ReportPriority priority, Pageable pageable);
    
    // Find pending reports
    Page<Report> findByStatusOrderByCreatedAtDesc(Report.ReportStatus status, Pageable pageable);
    
    // Count reports by status
    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status")
    Long countByStatus(@Param("status") Report.ReportStatus status);
    
    // Count reports for an entity
    @Query("SELECT COUNT(r) FROM Report r WHERE r.reportedEntityType = :entityType " +
           "AND r.reportedEntityId = :entityId AND r.status = :status")
    Long countByEntityAndStatus(
        @Param("entityType") Report.EntityType entityType,
        @Param("entityId") UUID entityId,
        @Param("status") Report.ReportStatus status
    );
    
    // Get reports statistics
    @Query("SELECT r.status, COUNT(r) FROM Report r GROUP BY r.status")
    List<Object[]> getReportStatsByStatus();
    
    @Query("SELECT r.reason, COUNT(r) FROM Report r GROUP BY r.reason")
    List<Object[]> getReportStatsByReason();
    
    @Query("SELECT r.priority, COUNT(r) FROM Report r WHERE r.status = :status GROUP BY r.priority")
    List<Object[]> getPendingReportsByPriority(@Param("status") Report.ReportStatus status);
    
    // Find high priority pending reports
    @Query("SELECT r FROM Report r WHERE r.status = 'PENDING' AND r.priority IN ('HIGH', 'CRITICAL') " +
           "ORDER BY r.priority DESC, r.createdAt ASC")
    List<Report> findUrgentReports();
    
    // Find reports created in date range
    @Query("SELECT r FROM Report r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    Page<Report> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Check if user has already reported an entity
    boolean existsByReporterIdAndReportedEntityTypeAndReportedEntityIdAndStatus(
        UUID reporterId,
        Report.EntityType entityType,
        UUID entityId,
        Report.ReportStatus status
    );
}
