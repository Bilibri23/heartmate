package org.rooms.roombay.repository;

import org.rooms.roombay.entity.ListingSearchOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ListingSearchOutboxRepository extends JpaRepository<ListingSearchOutbox, UUID> {

    @Query("SELECT o FROM ListingSearchOutbox o WHERE o.processedAt IS NULL ORDER BY o.createdAt ASC")
    List<ListingSearchOutbox> findPendingOrderByCreatedAtAsc(Pageable pageable);
}
