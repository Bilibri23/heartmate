package org.rooms.roombuddy.repository;

import jakarta.persistence.criteria.Predicate;
import org.rooms.roombuddy.entity.PropertyListing;
import org.springframework.data.jpa.domain.Specification;

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

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rentAmount"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rentAmount"), maxPrice));
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
