package org.rooms.roombuddy.search;

import java.util.Map;
import java.util.UUID;

/**
 * Abstraction for writing listing documents to the search index
 * (Elasticsearch/OpenSearch/Meilisearch). Implement with a no-op for DB-only
 * operation, or with a real client when the search cluster is available.
 */
public interface SearchIndexClient {

    /**
     * Index or update a listing document. Payload contains: id, title, description,
     * city, neighborhood, amenities, furnished, price, propertyType, latitude,
     * longitude, verified, featured, status.
     */
    void index(Map<String, Object> payload);

    /** Remove a listing from the index by id. */
    void delete(UUID listingId);
}
