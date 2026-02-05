package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.HouseRules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseRulesRepository extends JpaRepository<HouseRules, UUID> {
    
    Optional<HouseRules> findByHouseholdId(UUID householdId);
    
    boolean existsByHouseholdId(UUID householdId);
}
