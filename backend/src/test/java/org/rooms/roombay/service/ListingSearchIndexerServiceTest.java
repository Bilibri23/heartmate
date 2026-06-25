package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.entity.ListingSearchOutbox;
import org.rooms.roombay.repository.ListingSearchOutboxRepository;
import org.rooms.roombay.search.SearchIndexClient;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingSearchIndexerServiceTest {

    @Mock
    private ListingSearchOutboxRepository outboxRepository;
    @Mock
    private SearchIndexClient searchIndexClient;

    @InjectMocks
    private ListingSearchIndexerService indexerService;

    @Test
    void processPendingOutboxIndexesUpdatesAndMarksProcessed() {
        UUID listingId = UUID.randomUUID();
        ListingSearchOutbox row = ListingSearchOutbox.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .eventType(ListingSearchOutbox.EVENT_UPDATED)
                .payload(Map.of("id", listingId.toString(), "status", "ACTIVE"))
                .createdAt(LocalDateTime.now())
                .processingAttempts(0)
                .build();

        when(outboxRepository.findPendingOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(row));

        indexerService.processPendingOutbox();

        verify(searchIndexClient).index(row.getPayload());
        verify(searchIndexClient, never()).delete(any());
        assertThat(row.getProcessedAt()).isNotNull();
        verify(outboxRepository).save(row);
    }

    @Test
    void processPendingOutboxDeletesOnListingDeletedEvent() {
        UUID listingId = UUID.randomUUID();
        ListingSearchOutbox row = ListingSearchOutbox.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .eventType(ListingSearchOutbox.EVENT_DELETED)
                .payload(Map.of("id", listingId.toString()))
                .createdAt(LocalDateTime.now())
                .processingAttempts(0)
                .build();

        when(outboxRepository.findPendingOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(row));

        indexerService.processPendingOutbox();

        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(searchIndexClient).delete(idCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(listingId);
        assertThat(row.getProcessedAt()).isNotNull();
    }

    @Test
    void processPendingOutboxIncrementsAttemptsOnFailure() {
        ListingSearchOutbox row = ListingSearchOutbox.builder()
                .id(UUID.randomUUID())
                .listingId(UUID.randomUUID())
                .eventType(ListingSearchOutbox.EVENT_UPDATED)
                .payload(Map.of("id", "bad"))
                .createdAt(LocalDateTime.now())
                .processingAttempts(0)
                .build();

        when(outboxRepository.findPendingOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(row));
        org.mockito.Mockito.doThrow(new RuntimeException("ES down"))
                .when(searchIndexClient).index(any());

        indexerService.processPendingOutbox();

        assertThat(row.getProcessingAttempts()).isEqualTo(1);
        assertThat(row.getProcessedAt()).isNull();
        verify(outboxRepository).save(row);
    }
}
