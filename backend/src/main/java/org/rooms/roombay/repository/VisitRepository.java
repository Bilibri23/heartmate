package org.rooms.roombay.repository;

import org.rooms.roombay.entity.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VisitRepository extends JpaRepository<Visit, UUID> {

    Page<Visit> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Visit> findByTenantIdAndStatus(UUID tenantId, Visit.Status status, Pageable pageable);

    Page<Visit> findByLandlordId(UUID landlordId, Pageable pageable);

    Page<Visit> findByLandlordIdAndStatus(UUID landlordId, Visit.Status status, Pageable pageable);

    List<Visit> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    List<Visit> findByListingIdAndTenantIdOrderByCreatedAtAsc(UUID listingId, UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, Visit.Status status);

    long countByLandlordIdAndStatus(UUID landlordId, Visit.Status status);
}
