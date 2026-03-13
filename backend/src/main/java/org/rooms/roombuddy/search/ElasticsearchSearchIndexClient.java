package org.rooms.roombuddy.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Writes listing documents to Elasticsearch/OpenSearch. Active when
 * roombuddy.search.elasticsearch.enabled=true and spring.elasticsearch.uris is set.
 */
@Component("elasticsearchSearchIndexClient")
@Primary
@ConditionalOnProperty(name = "roombuddy.search.elasticsearch.enabled", havingValue = "true")
@ConditionalOnBean(ElasticsearchClient.class)
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSearchIndexClient implements SearchIndexClient {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${roombuddy.search.elasticsearch.index-name:listings}")
    private String indexName;

    @Override
    public void index(Map<String, Object> payload) {
        if (payload == null) return;
        String id = (String) payload.get("id");
        if (id == null || id.isBlank()) return;
        try {
            Map<String, Object> doc = new HashMap<>(payload);
            Object lat = doc.remove("latitude");
            Object lon = doc.remove("longitude");
            if (lat != null && lon != null) {
                doc.put("location", Map.of("lat", lat, "lon", lon));
            }
            IndexRequest<JsonData> req = IndexRequest.of(i ->
                i.index(indexName).id(id).document(JsonData.of(doc))
            );
            elasticsearchClient.index(req);
            log.debug("Indexed listing id={} into {}", id, indexName);
        } catch (ElasticsearchException e) {
            log.warn("Elasticsearch index failed for listing id={}: {}", id, e.getMessage());
            throw new RuntimeException("Search index update failed", e);
        } catch (Exception e) {
            log.warn("Index failed for listing id={}: {}", id, e.getMessage());
            throw new RuntimeException("Search index update failed", e);
        }
    }

    @Override
    public void delete(UUID listingId) {
        if (listingId == null) return;
        try {
            DeleteRequest req = DeleteRequest.of(d -> d.index(indexName).id(listingId.toString()));
            elasticsearchClient.delete(req);
            log.debug("Deleted listing id={} from {}", listingId, indexName);
        } catch (ElasticsearchException e) {
            log.warn("Elasticsearch delete failed for listing id={}: {}", listingId, e.getMessage());
            throw new RuntimeException("Search index delete failed", e);
        } catch (Exception e) {
            log.warn("Delete failed for listing id={}: {}", listingId, e.getMessage());
            throw new RuntimeException("Search index delete failed", e);
        }
    }
}
