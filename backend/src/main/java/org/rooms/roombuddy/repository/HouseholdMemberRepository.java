package org.rooms.roombay.repository;

import org.rooms.roombay.entity.HouseholdMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {
    
    List<HouseholdMember> findByHouseholdId(UUID householdId);
    
    @Query("SELECT m FROM HouseholdMember m WHERE m.household.id = :householdId AND m.leftAt IS NULL")
    List<HouseholdMember> findActiveByHouseholdId(@Param("householdId") UUID householdId);
    
    Optional<HouseholdMember> findByHouseholdIdAndUserId(UUID householdId, UUID userId);
    
    @Query("SELECT m FROM HouseholdMember m WHERE m.household.id = :householdId AND m.user.id = :userId AND m.leftAt IS NULL")
    Optional<HouseholdMember> findActiveByHouseholdIdAndUserId(@Param("householdId") UUID householdId, @Param("userId") UUID userId);
    
    @Query("SELECT COUNT(m) FROM HouseholdMember m WHERE m.household.id = :householdId AND m.leftAt IS NULL")
    long countActiveMembers(@Param("householdId") UUID householdId);
    
    @Query("SELECT m FROM HouseholdMember m WHERE m.user.id = :userId AND m.leftAt IS NULL")
    List<HouseholdMember> findActiveByUserId(@Param("userId") UUID userId);
}
