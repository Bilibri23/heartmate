package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.HouseholdTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HouseholdTaskRepository extends JpaRepository<HouseholdTask, UUID> {
    
    Page<HouseholdTask> findByHouseholdIdOrderByDueDateAsc(UUID householdId, Pageable pageable);
    
    @Query("SELECT t FROM HouseholdTask t WHERE t.household.id = :householdId AND t.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY t.dueDate ASC")
    List<HouseholdTask> findPendingByHouseholdId(@Param("householdId") UUID householdId);
    
    @Query("SELECT t FROM HouseholdTask t WHERE t.assignedTo.id = :userId AND t.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY t.dueDate ASC")
    List<HouseholdTask> findPendingByAssignedTo(@Param("userId") UUID userId);
    
    @Query("SELECT t FROM HouseholdTask t WHERE t.household.id = :householdId AND t.dueDate < :date AND t.status = 'PENDING'")
    List<HouseholdTask> findOverdue(@Param("householdId") UUID householdId, @Param("date") LocalDate date);
    
    @Query("SELECT t FROM HouseholdTask t WHERE t.household.id = :householdId AND t.dueDate = :date")
    List<HouseholdTask> findDueOn(@Param("householdId") UUID householdId, @Param("date") LocalDate date);
    
    @Query("SELECT t FROM HouseholdTask t WHERE t.household.id = :householdId AND t.status = :status")
    List<HouseholdTask> findByHouseholdIdAndStatus(@Param("householdId") UUID householdId, @Param("status") HouseholdTask.TaskStatus status);
    
    @Query("SELECT t FROM HouseholdTask t WHERE t.household.id = :householdId AND t.isRecurring = true")
    List<HouseholdTask> findRecurringByHouseholdId(@Param("householdId") UUID householdId);
    
    @Query("SELECT COUNT(t) FROM HouseholdTask t WHERE t.household.id = :householdId AND t.status = 'COMPLETED' AND t.completedBy.id = :userId")
    long countCompletedByUser(@Param("householdId") UUID householdId, @Param("userId") UUID userId);
    
    List<HouseholdTask> findByHouseholdId(UUID householdId);
}
