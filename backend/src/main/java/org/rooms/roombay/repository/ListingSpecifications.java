package org.rooms.roombay.repository;

import jakarta.persistence.criteria.Predicate;
import org.rooms.roombay.entity.PropertyListing;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification builder for PropertyListing search queries.
 * Pushes all filtering to the database instead of in-memory stream filtering,
 * enabling efficient search at scale (50k+ listings).
 */
public final class ListingSpecifications {

    private ListingSpecifications() {}

    public static Specification<PropertyListing> buildSearchSpec(
            String query,
            String city,
            String neighborhood,
            String propertyType,
            Integer minPrice,
            Integer maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            List<String> amenities,
            String availableFrom) {
        return buildSearchSpec(
                query, city, neighborhood, propertyType,
                minPrice, maxPrice, bedrooms, bathrooms,
                amenities, availableFrom,
                null, null, null, null);
    }

    public static Specification<PropertyListing> buildSearchSpec(
            String query,
            String city,
            String neighborhood,
            String propertyType,
            Integer minPrice,
            Integer maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            List<String> amenities,
            String availableFrom,
            String listingPurpose,
            String stayType,
            String furnishingStatus,
            Boolean sharedAccommodation) {
        return buildSearchSpec(
                query, city, neighborhood, propertyType,
                minPrice, maxPrice, bedrooms, bathrooms,
                amenities, availableFrom,
                listingPurpose, stayType, furnishingStatus, sharedAccommodation,
                null, null, null, null);
    }

    /**
     * Full search spec with an optional geographic bounding box (for "search this area" and as the
     * fallback approximation of the "Near me" radius). Listings missing coordinates are excluded
     * when a box is supplied.
     */
    public static Specification<PropertyListing> buildSearchSpec(
            String query,
            String city,
            String neighborhood,
            String propertyType,
            Integer minPrice,
            Integer maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            List<String> amenities,
            String availableFrom,
            String listingPurpose,
            String stayType,
            String furnishingStatus,
            Boolean sharedAccommodation,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng) {

        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), PropertyListing.Status.ACTIVE));
            predicates.add(cb.isTrue(root.get("verified")));

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("city")), pattern),
                        cb.like(cb.lower(root.get("neighborhood")), pattern)
                ));
            }

            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
            }

            if (neighborhood != null && !neighborhood.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("neighborhood")), neighborhood.toLowerCase()));
            }

            if (propertyType != null && !propertyType.isBlank()) {
                try {
                    PropertyListing.PropertyType type = PropertyListing.PropertyType.valueOf(propertyType.toUpperCase());
                    predicates.add(cb.equal(root.get("propertyType"), type));
                } catch (IllegalArgumentException ignored) {
                }
            }

            PropertyListing.ListingPurpose purposeFilter = parseListingPurpose(listingPurpose);
            if (purposeFilter != null) {
                predicates.add(cb.equal(root.get("listingPurpose"), purposeFilter));
            }

            if (stayType != null && !stayType.isBlank()) {
                try {
                    PropertyListing.StayType stay = PropertyListing.StayType.valueOf(stayType.toUpperCase());
                    predicates.add(cb.equal(root.get("stayType"), stay));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (furnishingStatus != null && !furnishingStatus.isBlank()) {
                try {
                    PropertyListing.FurnishingStatus furnishing = PropertyListing.FurnishingStatus.valueOf(furnishingStatus.toUpperCase());
                    predicates.add(cb.equal(root.get("furnishingStatus"), furnishing));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (sharedAccommodation != null) {
                predicates.add(cb.equal(root.get("isSharedAccommodation"), sharedAccommodation));
            }

            if (minPrice != null || maxPrice != null) {
                if (purposeFilter == PropertyListing.ListingPurpose.SALE) {
                    if (minPrice != null) {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("salePrice"), minPrice));
                    }
                    if (maxPrice != null) {
                        predicates.add(cb.lessThanOrEqualTo(root.get("salePrice"), maxPrice));
                    }
                } else if (purposeFilter == PropertyListing.ListingPurpose.RENT) {
                    if (minPrice != null) {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("rentAmount"), minPrice));
                    }
                    if (maxPrice != null) {
                        predicates.add(cb.lessThanOrEqualTo(root.get("rentAmount"), maxPrice));
                    }
                } else {
                    List<Predicate> pricePredicates = new ArrayList<>();
                    if (minPrice != null || maxPrice != null) {
                        List<Predicate> rentPrice = new ArrayList<>();
                        if (minPrice != null) {
                            rentPrice.add(cb.greaterThanOrEqualTo(root.get("rentAmount"), minPrice));
                        }
                        if (maxPrice != null) {
                            rentPrice.add(cb.lessThanOrEqualTo(root.get("rentAmount"), maxPrice));
                        }
                        pricePredicates.add(cb.and(rentPrice.toArray(new Predicate[0])));

                        List<Predicate> salePrice = new ArrayList<>();
                        if (minPrice != null) {
                            salePrice.add(cb.greaterThanOrEqualTo(root.get("salePrice"), minPrice));
                        }
                        if (maxPrice != null) {
                            salePrice.add(cb.lessThanOrEqualTo(root.get("salePrice"), maxPrice));
                        }
                        pricePredicates.add(cb.and(salePrice.toArray(new Predicate[0])));
                        predicates.add(cb.or(pricePredicates.toArray(new Predicate[0])));
                    }
                }
            }

            if (bedrooms != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), bedrooms));
            }

            if (bathrooms != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bathrooms"), bathrooms));
            }

            if (availableFrom != null && !availableFrom.isBlank()) {
                try {
                    LocalDate date = LocalDate.parse(availableFrom);
                    predicates.add(cb.or(
                            cb.isNull(root.get("availableFrom")),
                            cb.lessThanOrEqualTo(root.get("availableFrom"), date)
                    ));
                } catch (Exception ignored) {
                }
            }

            if (minLat != null && maxLat != null && minLng != null && maxLng != null) {
                predicates.add(cb.isNotNull(root.get("latitude")));
                predicates.add(cb.isNotNull(root.get("longitude")));
                predicates.add(cb.between(root.<BigDecimal>get("latitude"),
                        BigDecimal.valueOf(Math.min(minLat, maxLat)), BigDecimal.valueOf(Math.max(minLat, maxLat))));
                predicates.add(cb.between(root.<BigDecimal>get("longitude"),
                        BigDecimal.valueOf(Math.min(minLng, maxLng)), BigDecimal.valueOf(Math.max(minLng, maxLng))));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static PropertyListing.ListingPurpose parseListingPurpose(String listingPurpose) {
        if (listingPurpose == null || listingPurpose.isBlank()) {
            return null;
        }
        try {
            return PropertyListing.ListingPurpose.valueOf(listingPurpose.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
