package org.rooms.roombay.repository;

import org.rooms.roombay.entity.RealtorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RealtorProfileRepository extends JpaRepository<RealtorProfile, UUID> {

    Optional<RealtorProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    Page<RealtorProfile> findByVerificationStatus(RealtorProfile.VerificationStatus status, Pageable pageable);

    long countByVerificationStatus(RealtorProfile.VerificationStatus status);
}
