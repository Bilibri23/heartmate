package org.rooms.roombay.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.service.ListingService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * On startup, when Elasticsearch is enabled and the index is empty, enqueues all
 * ACTIVE listings so the indexer can populate the search index. Fixes the case
 * where listings exist in DB but were never indexed (e.g. created before outbox).
 */
@Component
@Order(100)
@ConditionalOnProperty(name = "roombay.search.elasticsearch.enabled", havingValue = "true")
@ConditionalOnBean(ElasticsearchSearchService.class)
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchBulkIndexRunner implements ApplicationRunner {

    private final Optional<ElasticsearchSearchService> elasticsearchSearchService;
    private final ListingService listingService;

    @Override
    public void run(ApplicationArguments args) {
        if (elasticsearchSearchService.isEmpty()) return;
        try {
            long count = elasticsearchSearchService.get().count();
            if (count == 0) {
                log.info("Elasticsearch index is empty - enqueueing all ACTIVE listings for indexing");
                int enqueued = listingService.bulkEnqueueActiveListingsForSearch();
                if (enqueued > 0) {
                    log.info("Enqueued {} listings; indexer will process within ~15 seconds", enqueued);
                }
            }
        } catch (Exception e) {
            log.warn("Bulk index check failed (indexer will process outbox): {}", e.getMessage());
        }
    }
}
