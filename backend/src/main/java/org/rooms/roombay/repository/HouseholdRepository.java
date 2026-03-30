package org.rooms.roombay.repository;

import org.rooms.roombay.entity.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, UUID> {
    
    Optional<Household> findByLeaseId(UUID leaseId);
    
    Optional<Household> findByListingId(UUID listingId);
    
    @Query("SELECT h FROM Household h JOIN h.members m WHERE m.user.id = :userId AND m.leftAt IS NULL")
    List<Household> findByMemberUserId(@Param("userId") UUID userId);
    
    @Query("SELECT h FROM Household h JOIN h.members m WHERE m.user.id = :userId AND h.status = 'ACTIVE' AND m.leftAt IS NULL")
    List<Household> findActiveByMemberUserId(@Param("userId") UUID userId);
    
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM HouseholdMember m WHERE m.household.id = :householdId AND m.user.id = :userId AND m.leftAt IS NULL")
    boolean isMember(@Param("householdId") UUID householdId, @Param("userId") UUID userId);
    
    @Query("SELECT h FROM Household h WHERE h.listing.id = :listingId AND h.status = 'ACTIVE'")
    Optional<Household> findActiveByListingId(@Param("listingId") UUID listingId);
}
