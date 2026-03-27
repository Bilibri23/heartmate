package org.rooms.roombay.repository;

import org.rooms.roombay.entity.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SavedSearchRepository extends JpaRepository<SavedSearch, UUID> {
    
    List<SavedSearch> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    List<SavedSearch> findByNotifyNewListingsTrue();
    
    List<SavedSearch> findByNotifyPriceDropsTrue();
}
