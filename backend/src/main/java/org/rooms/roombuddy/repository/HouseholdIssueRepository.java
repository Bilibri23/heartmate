package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.HouseholdIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HouseholdIssueRepository extends JpaRepository<HouseholdIssue, UUID> {
    
    Page<HouseholdIssue> findByHouseholdIdOrderByCreatedAtDesc(UUID householdId, Pageable pageable);
    
    @Query("SELECT i FROM HouseholdIssue i WHERE i.household.id = :householdId AND i.status NOT IN ('RESOLVED', 'CLOSED') ORDER BY i.severity DESC, i.createdAt ASC")
    List<HouseholdIssue> findOpenByHouseholdId(@Param("householdId") UUID householdId);
    
    @Query("SELECT i FROM HouseholdIssue i WHERE i.reportedBy.id = :userId ORDER BY i.createdAt DESC")
    List<HouseholdIssue> findByReportedBy(@Param("userId") UUID userId);
    
    @Query("SELECT i FROM HouseholdIssue i WHERE i.reportedAgainst.id = :userId ORDER BY i.createdAt DESC")
    List<HouseholdIssue> findByReportedAgainst(@Param("userId") UUID userId);
    
    @Query("SELECT i FROM HouseholdIssue i WHERE i.isEscalated = true AND i.status = 'ESCALATED' ORDER BY i.escalatedAt ASC")
    List<HouseholdIssue> findEscalated();
    
    @Query("SELECT i FROM HouseholdIssue i WHERE i.household.id = :householdId AND i.status = :status")
    List<HouseholdIssue> findByHouseholdIdAndStatus(@Param("householdId") UUID householdId, @Param("status") HouseholdIssue.IssueStatus status);
    
    @Query("SELECT COUNT(i) FROM HouseholdIssue i WHERE i.household.id = :householdId AND i.status NOT IN ('RESOLVED', 'CLOSED')")
    long countOpenIssues(@Param("householdId") UUID householdId);
    
    List<HouseholdIssue> findByHouseholdId(UUID householdId);
}
