package org.rooms.roombay.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ensures the listings index exists with explicit mappings (geo_point, keyword filters, date recency).
 */
@Component
@Order(50)
@ConditionalOnProperty(name = "roombay.search.elasticsearch.enabled", havingValue = "true")
@ConditionalOnBean(ElasticsearchClient.class)
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexMappingInitializer implements ApplicationRunner {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${roombay.search.elasticsearch.index-name:listings}")
    private String indexName;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(indexName)))
                    .value();
            if (exists) {
                log.debug("Elasticsearch index '{}' already exists", indexName);
                return;
            }

            TypeMapping mapping = TypeMapping.of(m -> m
                    .properties("id", Property.of(p -> p.keyword(k -> k)))
                    .properties("title", Property.of(p -> p.text(t -> t.analyzer("standard"))))
                    .properties("description", Property.of(p -> p.text(t -> t.analyzer("standard"))))
                    .properties("city", Property.of(p -> p.keyword(k -> k)))
                    .properties("neighborhood", Property.of(p -> p.keyword(k -> k)))
                    .properties("propertyType", Property.of(p -> p.keyword(k -> k)))
                    .properties("status", Property.of(p -> p.keyword(k -> k)))
                    .properties("amenities", Property.of(p -> p.keyword(k -> k)))
                    .properties("furnished", Property.of(p -> p.boolean_(b -> b)))
                    .properties("verified", Property.of(p -> p.boolean_(b -> b)))
                    .properties("featured", Property.of(p -> p.boolean_(b -> b)))
                    .properties("price", Property.of(p -> p.long_(l -> l)))
                    .properties("bedrooms", Property.of(p -> p.integer(i -> i)))
                    .properties("bathrooms", Property.of(p -> p.integer(i -> i)))
                    .properties("viewsCount", Property.of(p -> p.long_(l -> l)))
                    .properties("createdAt", Property.of(p -> p.date(d -> d)))
                    .properties("location", Property.of(p -> p.geoPoint(g -> g)))
            );

            elasticsearchClient.indices().create(CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .mappings(mapping)
            ));
            log.info("Created Elasticsearch index '{}' with explicit geo_point and keyword mappings", indexName);
        } catch (Exception e) {
            log.warn("Could not ensure Elasticsearch index mapping for '{}': {}", indexName, e.getMessage());
        }
    }
}
