package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {
    
    Optional<Dispute> findByReferenceCode(String referenceCode);
    
    Page<Dispute> findByOpenedById(UUID userId, Pageable pageable);
    
    Page<Dispute> findByAgainstUserId(UUID userId, Pageable pageable);
    
    Page<Dispute> findByLeaseId(UUID leaseId, Pageable pageable);
    
    Page<Dispute> findByStatus(Dispute.DisputeStatus status, Pageable pageable);
    
    Page<Dispute> findByAssignedToId(UUID adminId, Pageable pageable);
    
    @Query("SELECT d FROM Dispute d WHERE d.openedBy.id = :userId OR d.againstUser.id = :userId")
    Page<Dispute> findByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT d FROM Dispute d WHERE d.status IN ('OPEN', 'UNDER_REVIEW', 'AWAITING_RESPONSE', 'MEDIATION') ORDER BY d.priority DESC, d.createdAt ASC")
    Page<Dispute> findActiveDisputes(Pageable pageable);
    
    @Query("SELECT d FROM Dispute d WHERE d.priority = 'URGENT' AND d.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Dispute> findUrgentDisputes();
    
    long countByStatus(Dispute.DisputeStatus status);
    
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status NOT IN ('RESOLVED', 'CLOSED')")
    long countOpenDisputes();
    
    boolean existsByLeaseIdAndStatusNotIn(UUID leaseId, List<Dispute.DisputeStatus> closedStatuses);
}
