package org.rooms.roombay.repository;

import org.rooms.roombay.entity.PlatformSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformSettingsRepository extends JpaRepository<PlatformSettings, UUID> {
    Optional<PlatformSettings> findFirstByOrderByCreatedAtAsc();
}
