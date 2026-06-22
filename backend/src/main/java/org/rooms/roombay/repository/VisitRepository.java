package org.rooms.roombay.repository;

import org.rooms.roombay.entity.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("SELECT COUNT(v) FROM Visit v WHERE v.landlord.id = :landlordId " +
           "AND v.createdAt >= :start AND v.createdAt < :end")
    long countByLandlordIdAndCreatedAtBetween(
            @Param("landlordId") UUID landlordId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(v) FROM Visit v WHERE v.landlord.id = :landlordId " +
           "AND v.status = :status AND v.createdAt >= :start AND v.createdAt < :end")
    long countByLandlordIdAndStatusAndCreatedAtBetween(
            @Param("landlordId") UUID landlordId,
            @Param("status") Visit.Status status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
