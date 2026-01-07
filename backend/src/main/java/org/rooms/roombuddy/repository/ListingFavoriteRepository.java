package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.ListingFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingFavoriteRepository extends JpaRepository<ListingFavorite, UUID> {
    
    Optional<ListingFavorite> findByUserIdAndListingId(UUID userId, UUID listingId);
    
    boolean existsByUserIdAndListingId(UUID userId, UUID listingId);
    
    List<ListingFavorite> findByUserId(UUID userId);
    
    @Query("SELECT COUNT(f) FROM ListingFavorite f WHERE f.listing.id = :listingId")
    Long countByListingId(@Param("listingId") UUID listingId);
    
    void deleteByUserIdAndListingId(UUID userId, UUID listingId);
}

