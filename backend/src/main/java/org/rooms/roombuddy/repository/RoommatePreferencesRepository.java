package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.RoommatePreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoommatePreferencesRepository extends JpaRepository<RoommatePreferences, UUID> {
    Optional<RoommatePreferences> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    void deleteByUserId(UUID userId);
    List<RoommatePreferences> findByLookingForRoommate(Boolean lookingForRoommate);
}

