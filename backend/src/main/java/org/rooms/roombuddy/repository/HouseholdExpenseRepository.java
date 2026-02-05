package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.HouseholdExpense;
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
public interface HouseholdExpenseRepository extends JpaRepository<HouseholdExpense, UUID> {
    
    Page<HouseholdExpense> findByHouseholdIdOrderByExpenseDateDesc(UUID householdId, Pageable pageable);
    
    List<HouseholdExpense> findByHouseholdIdAndExpenseDateBetween(UUID householdId, LocalDate start, LocalDate end);
    
    @Query("SELECT e FROM HouseholdExpense e WHERE e.household.id = :householdId AND e.category = :category ORDER BY e.expenseDate DESC")
    List<HouseholdExpense> findByHouseholdIdAndCategory(@Param("householdId") UUID householdId, @Param("category") HouseholdExpense.ExpenseCategory category);
    
    @Query("SELECT e FROM HouseholdExpense e WHERE e.paidBy.id = :userId ORDER BY e.expenseDate DESC")
    List<HouseholdExpense> findByPaidByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT SUM(e.amount) FROM HouseholdExpense e WHERE e.household.id = :householdId AND e.expenseDate BETWEEN :start AND :end")
    Long getTotalExpensesForPeriod(@Param("householdId") UUID householdId, @Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT e.category, SUM(e.amount) FROM HouseholdExpense e WHERE e.household.id = :householdId AND e.expenseDate BETWEEN :start AND :end GROUP BY e.category")
    List<Object[]> getExpensesByCategory(@Param("householdId") UUID householdId, @Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT e FROM HouseholdExpense e WHERE e.household.id = :householdId AND e.isRecurring = true")
    List<HouseholdExpense> findRecurringByHouseholdId(@Param("householdId") UUID householdId);
    
    List<HouseholdExpense> findByHouseholdId(UUID householdId);
}
