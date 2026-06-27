-- Align property_listings.average_rating with the entity mapping.
-- V42 created it as DECIMAL(3,2), but PropertyListing.averageRating is a Double, so
-- Hibernate ddl-auto=validate (prod + CI migration-check) fails with a wrong-column-type
-- error and the application context cannot start. Switch to double precision
-- (value-preserving) so the schema matches the entity.
ALTER TABLE property_listings
    ALTER COLUMN average_rating TYPE double precision USING average_rating::double precision;
