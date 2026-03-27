package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.entity.ListingSearchOutbox;
import org.rooms.roombay.repository.ListingSearchOutboxRepository;
import org.rooms.roombay.search.SearchIndexClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Background worker that processes the listing_search_outbox table and sends
 * events to the search index (Elasticsearch/OpenSearch/Meilisearch). Runs
 * periodically; when no real search client is configured, NoOpSearchIndexClient
 * simply marks outbox rows as processed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListingSearchIndexerService {

    private final ListingSearchOutboxRepository outboxRepository;
    private final SearchIndexClient searchIndexClient;

    @Value("${roombay.search.indexer.batch-size:50}")
    private int batchSize = 50;

    @Scheduled(fixedDelayString = "${roombay.search.indexer.interval-ms:15000}")
    @Transactional
    public void processPendingOutbox() {
        List<ListingSearchOutbox> pending = outboxRepository.findPendingOrderByCreatedAtAsc(PageRequest.of(0, batchSize));
        if (pending.isEmpty()) return;
        int indexed = 0, failed = 0;
        for (ListingSearchOutbox row : pending) {
            try {
                if (ListingSearchOutbox.EVENT_DELETED.equals(row.getEventType())) {
                    searchIndexClient.delete(row.getListingId());
                } else {
                    searchIndexClient.index(row.getPayload());
                    indexed++;
                }
                row.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(row);
            } catch (Exception e) {
                failed++;
                log.warn("Search indexer failed for outbox id={}, listingId={}, eventType={}: {}",
                        row.getId(), row.getListingId(), row.getEventType(), e.getMessage());
                row.setProcessingAttempts(row.getProcessingAttempts() + 1);
                outboxRepository.save(row);
            }
        }
        if (indexed > 0 || failed > 0) {
            log.info("Search indexer: processed {} rows (indexed={}, failed={})", pending.size(), indexed, failed);
        }
    }
}
