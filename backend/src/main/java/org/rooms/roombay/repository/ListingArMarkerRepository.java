package org.rooms.roombay.repository;

import org.rooms.roombay.entity.ListingArMarker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListingArMarkerRepository extends JpaRepository<ListingArMarker, UUID> {
    List<ListingArMarker> findByListingIdOrderByCreatedAtAsc(UUID listingId);
    void deleteByListingId(UUID listingId);
}
