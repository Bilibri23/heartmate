package org.rooms.roombay.repository;

import org.rooms.roombay.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {
    
    List<ExpenseSplit> findByExpenseId(UUID expenseId);
    
    Optional<ExpenseSplit> findByExpenseIdAndUserId(UUID expenseId, UUID userId);
    
    @Query("SELECT s FROM ExpenseSplit s WHERE s.user.id = :userId AND s.isPaid = false")
    List<ExpenseSplit> findUnpaidByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT s FROM ExpenseSplit s WHERE s.expense.household.id = :householdId AND s.user.id = :userId AND s.isPaid = false")
    List<ExpenseSplit> findUnpaidByHouseholdAndUser(@Param("householdId") UUID householdId, @Param("userId") UUID userId);
    
    @Query("SELECT SUM(s.amount) FROM ExpenseSplit s WHERE s.user.id = :userId AND s.isPaid = false")
    Long getTotalOwedByUser(@Param("userId") UUID userId);
    
    @Query("SELECT SUM(s.amount) FROM ExpenseSplit s WHERE s.expense.household.id = :householdId AND s.user.id = :userId AND s.isPaid = false")
    Long getTotalOwedInHousehold(@Param("householdId") UUID householdId, @Param("userId") UUID userId);
    
    @Query("SELECT SUM(e.amount) FROM HouseholdExpense e WHERE e.household.id = :householdId AND e.paidBy.id = :userId")
    Long getTotalPaidByUserInHousehold(@Param("householdId") UUID householdId, @Param("userId") UUID userId);
}
