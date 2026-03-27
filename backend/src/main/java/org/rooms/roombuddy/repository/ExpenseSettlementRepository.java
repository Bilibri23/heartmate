package org.rooms.roombay.repository;

import org.rooms.roombay.entity.ExpenseSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseSettlementRepository extends JpaRepository<ExpenseSettlement, UUID> {
    
    List<ExpenseSettlement> findByHouseholdIdOrderBySettledAtDesc(UUID householdId);
    
    @Query("SELECT s FROM ExpenseSettlement s WHERE s.fromUser.id = :userId OR s.toUser.id = :userId ORDER BY s.settledAt DESC")
    List<ExpenseSettlement> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT s FROM ExpenseSettlement s WHERE s.household.id = :householdId AND (s.fromUser.id = :userId OR s.toUser.id = :userId) ORDER BY s.settledAt DESC")
    List<ExpenseSettlement> findByHouseholdAndUser(@Param("householdId") UUID householdId, @Param("userId") UUID userId);
    
    @Query("SELECT SUM(s.amount) FROM ExpenseSettlement s WHERE s.household.id = :householdId AND s.fromUser.id = :fromUserId AND s.toUser.id = :toUserId")
    Long getTotalSettledBetweenUsers(@Param("householdId") UUID householdId, @Param("fromUserId") UUID fromUserId, @Param("toUserId") UUID toUserId);
    
    List<ExpenseSettlement> findByHouseholdIdAndSettledAtBetween(UUID householdId, LocalDateTime start, LocalDateTime end);
    
    List<ExpenseSettlement> findByHouseholdId(UUID householdId);
}
