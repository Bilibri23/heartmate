package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.BillReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BillReminderRepository extends JpaRepository<BillReminder, UUID> {
    
    List<BillReminder> findByHouseholdId(UUID householdId);
    
    @Query("SELECT b FROM BillReminder b WHERE b.household.id = :householdId AND b.isActive = true")
    List<BillReminder> findActiveByHouseholdId(@Param("householdId") UUID householdId);
    
    @Query("SELECT b FROM BillReminder b WHERE b.isActive = true AND b.isRecurring = true AND b.dueDay = :dayOfMonth")
    List<BillReminder> findDueOnDay(@Param("dayOfMonth") int dayOfMonth);
    
    @Query("SELECT b FROM BillReminder b WHERE b.isActive = true AND b.isRecurring = true AND b.dueDay = :dayOfMonth AND b.reminderDaysBefore >= :daysUntilDue")
    List<BillReminder> findNeedingReminder(@Param("dayOfMonth") int dayOfMonth, @Param("daysUntilDue") int daysUntilDue);
}
