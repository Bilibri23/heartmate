package org.rooms.roombay.repository;

import org.rooms.roombay.entity.ListingPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingPreferencesRepository extends JpaRepository<ListingPreferences, UUID> {
    Optional<ListingPreferences> findByUserId(UUID userId);
}
