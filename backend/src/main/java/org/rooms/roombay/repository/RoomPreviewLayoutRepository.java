package org.rooms.roombay.repository;

import org.rooms.roombay.entity.RoomPreviewLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomPreviewLayoutRepository extends JpaRepository<RoomPreviewLayout, UUID> {
    List<RoomPreviewLayout> findByUserIdAndListingIdOrderByUpdatedAtDesc(UUID userId, UUID listingId);
}
