package org.rooms.roombay.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * No-op implementation when Elasticsearch is disabled or unavailable. Outbox events
 * are still processed (marked as processed). When ElasticsearchSearchIndexClient
 * exists (ES enabled and connected), it is used instead.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "roombay.search.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSearchIndexClient implements SearchIndexClient {

    @Override
    public void index(Map<String, Object> payload) {
        log.trace("Search index (no-op): index listing id={}", payload != null ? payload.get("id") : null);
    }

    @Override
    public void delete(UUID listingId) {
        log.trace("Search index (no-op): delete listing id={}", listingId);
    }
}
