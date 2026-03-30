package org.rooms.roombay.repository;

import org.rooms.roombay.entity.RuleAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleAgreementRepository extends JpaRepository<RuleAgreement, UUID> {
    
    List<RuleAgreement> findByHouseRulesId(UUID houseRulesId);
    
    Optional<RuleAgreement> findByHouseRulesIdAndUserId(UUID houseRulesId, UUID userId);
    
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM RuleAgreement a WHERE a.houseRules.id = :rulesId AND a.user.id = :userId")
    boolean hasAgreed(@Param("rulesId") UUID rulesId, @Param("userId") UUID userId);
    
    @Query("SELECT COUNT(a) FROM RuleAgreement a WHERE a.houseRules.id = :rulesId")
    long countAgreements(@Param("rulesId") UUID rulesId);
}
