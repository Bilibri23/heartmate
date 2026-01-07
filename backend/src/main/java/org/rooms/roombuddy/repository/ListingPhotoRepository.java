package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.ListingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingPhotoRepository extends JpaRepository<ListingPhoto, UUID> {
    
    List<ListingPhoto> findByListingIdOrderByDisplayOrderAsc(UUID listingId);
    
    List<ListingPhoto> findByListingId(UUID listingId);
    
    Optional<ListingPhoto> findByListingIdAndIsPrimary(UUID listingId, Boolean isPrimary);
    
    Optional<ListingPhoto> findFirstByListingIdOrderByDisplayOrderAsc(UUID listingId);
    
    @Query("SELECT p FROM ListingPhoto p WHERE p.listing.id = :listingId AND p.isPrimary = true")
    ListingPhoto findPrimaryPhotoByListingId(@Param("listingId") UUID listingId);
    
    @Modifying
    @Query("DELETE FROM ListingPhoto p WHERE p.listing.id = :listingId")
    void deleteByListingId(@Param("listingId") UUID listingId);
}

