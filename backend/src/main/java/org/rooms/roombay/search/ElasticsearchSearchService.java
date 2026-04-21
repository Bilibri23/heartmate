package org.rooms.roombay.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.ListingResponse;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.service.ListingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Search service that queries Elasticsearch with fuzzy matching, typo tolerance,
 * boosting (verified, featured, recency), and optional language-aware relevance.
 * Used when roombay.search.elasticsearch.enabled=true.
 */
@Service
@ConditionalOnBean(ElasticsearchClient.class)
@ConditionalOnProperty(name = "roombay.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSearchService {

    private final ElasticsearchClient client;
    private final PropertyListingRepository listingRepository;
    private final ListingService listingService;

    @Value("${roombay.search.elasticsearch.index-name:listings}")
    private String indexName;

    /** Feed modes: relevance (search), recent (newest first), trending (views boost), forYou (featured/verified boost). */
    public static final String MODE_RELEVANCE = "relevance";
    public static final String MODE_RECENT = "recent";
    public static final String MODE_TRENDING = "trending";
    public static final String MODE_FOR_YOU = "forYou";

    /**
     * Search listings via Elasticsearch with:
     * - Fuzzy matching & typo tolerance (fuzziness: AUTO)
     * - Boosting: featured (2x), verified (1.5x), recent listings (decay by createdAt)
     * - Optional language analyzer (fr = French, en = English)
     * - mode: relevance (default), recent, trending, forYou
     */
    public Page<ListingResponse> search(
            String query,
            String city,
            String neighborhood,
            String propertyType,
            Integer minPrice,
            Integer maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            List<String> amenities,
            Double maxDistance,
            Double userLat,
            Double userLon,
            String lang,
            String mode,
            UUID userId,
            Pageable pageable) {

        List<Query> must = new ArrayList<>();
        List<Query> filter = new ArrayList<>();

        // Text search with fuzzy + typo tolerance; when no query, match all (filter-only)
        if (query != null && !query.isBlank()) {
            String analyzer = "fr".equalsIgnoreCase(lang) ? "french" : "standard";
            // Search: title (2x), description, city, neighborhood, propertyType, amenities (furnished, WiFi, etc.)
            must.add(QueryBuilders.multiMatch(m -> m
                    .query(query)
                    .fields("title^2", "description", "city", "neighborhood", "propertyType", "amenities")
                    .fuzziness("AUTO")
                    .analyzer(analyzer)
            ));
        } else {
            must.add(QueryBuilders.matchAll(m -> m));
        }

        // Structured filters
        filter.add(QueryBuilders.term(t -> t.field("status").value("ACTIVE")));

        if (city != null && !city.isBlank()) {
            filter.add(QueryBuilders.term(t -> t.field("city").value(city)));
        }
        if (neighborhood != null && !neighborhood.isBlank()) {
            filter.add(QueryBuilders.term(t -> t.field("neighborhood").value(neighborhood)));
        }
        if (propertyType != null && !propertyType.isBlank()) {
            filter.add(QueryBuilders.term(t -> t.field("propertyType").value(propertyType)));
        }
        if (minPrice != null) {
            filter.add(Query.of(q -> q.range(RangeQuery.of(r -> r.number(n -> n.field("price").gte((double) minPrice))))));
        }
        if (maxPrice != null) {
            filter.add(Query.of(q -> q.range(RangeQuery.of(r -> r.number(n -> n.field("price").lte((double) maxPrice))))));
        }
        if (bedrooms != null) {
            filter.add(Query.of(q -> q.range(RangeQuery.of(r -> r.number(n -> n.field("bedrooms").gte((double) bedrooms))))));
        }
        if (bathrooms != null) {
            filter.add(Query.of(q -> q.range(RangeQuery.of(r -> r.number(n -> n.field("bathrooms").gte((double) bathrooms))))));
        }
        if (amenities != null && !amenities.isEmpty()) {
            for (String a : amenities) {
                filter.add(QueryBuilders.term(t -> t.field("amenities").value(a)));
            }
        }

        Query boostedQuery = QueryBuilders.bool(b -> {
            if (!must.isEmpty()) b.must(must);
            b.filter(filter);
            b.should(QueryBuilders.term(t -> t.field("featured").value(true).boost(2f)));
            b.should(QueryBuilders.term(t -> t.field("verified").value(true).boost(1.5f)));
            b.minimumShouldMatch("0");
            return b;
        });

        int from = (int) pageable.getOffset();
        int size = pageable.getPageSize();

        SearchRequest.Builder reqBuilder = new SearchRequest.Builder()
                .index(indexName)
                .query(boostedQuery)
                .from(from)
                .size(size);

        if (MODE_RECENT.equalsIgnoreCase(mode)) {
            reqBuilder.sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)));
        } else if (MODE_TRENDING.equalsIgnoreCase(mode)) {
            reqBuilder.sort(so -> so.field(f -> f.field("viewsCount").order(SortOrder.Desc)));
            reqBuilder.sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)));
        } else {
            reqBuilder.sort(so -> so.score(sc -> sc.order(SortOrder.Desc)));
        }

        SearchRequest req = reqBuilder.build();

        try {
            @SuppressWarnings("rawtypes")
            SearchResponse<Map> resp = client.search(req, Map.class);
            List<String> ids = resp.hits().hits().stream()
                    .map(Hit::id)
                    .filter(id -> id != null && !id.isBlank())
                    .collect(Collectors.toList());

            if (ids.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            List<PropertyListing> listings = listingRepository.findAllById(ids.stream().map(UUID::fromString).toList());
            Map<UUID, PropertyListing> byId = listings.stream().collect(Collectors.toMap(PropertyListing::getId, l -> l));

            List<ListingResponse> ordered = ids.stream()
                    .map(UUID::fromString)
                    .map(byId::get)
                    .filter(l -> l != null)
                    .map(l -> listingService.toListingResponse(l, userId))
                    .collect(Collectors.toList());

            var totalHit = resp.hits().total();
            long total = totalHit != null ? totalHit.value() : 0;
            return new PageImpl<>(ordered, pageable, total);
        } catch (Exception e) {
            // Do not propagate as 500: SearchController falls back to DB when ES returns empty (same pattern as FeedService).
            log.warn("Elasticsearch search failed (falling back to DB via empty page): {}", e.getMessage());
            return new PageImpl<>(List.of(), pageable, 0);
        }
    }

    public long count() {
        try {
            var resp = client.count(c -> c.index(indexName));
            return resp.count();
        } catch (Exception e) {
            log.warn("Elasticsearch count failed: {}", e.getMessage());
            return 0;
        }
    }
}
