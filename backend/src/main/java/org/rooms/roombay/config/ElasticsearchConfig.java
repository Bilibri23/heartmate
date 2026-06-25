package org.rooms.roombay.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.RestClients;

import java.net.URI;

/**
 * Configures ElasticsearchClient bean when roombay.search.elasticsearch.enabled=true.
 * Spring Data Elasticsearch requires this explicit configuration to create the client;
 * spring.elasticsearch.uris alone does not auto-create the bean.
 */
@Configuration
@ConditionalOnProperty(name = "roombay.search.elasticsearch.enabled", havingValue = "true")
@Slf4j
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String uris;

    @Override
    public ClientConfiguration clientConfiguration() {
        String hostPort = parseHostAndPort(uris);
        log.info("Elasticsearch configured: {} (OpenSearch-compatible JSON headers)", hostPort);
        // ES 8 client sends vendor Content-Type; OpenSearch rejects it (406) or duplicate headers (400).
        // Interceptor runs last and replaces Content-Type/Accept with plain application/json.
        return ClientConfiguration.builder()
                .connectedTo(hostPort)
                .withClientConfigurer(RestClients.RestClientConfigurationCallback.from(restClientBuilder -> {
                    restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                            httpClientBuilder.addInterceptorLast(openSearchHeaderInterceptor()));
                    return restClientBuilder;
                }))
                .build();
    }

    private static HttpRequestInterceptor openSearchHeaderInterceptor() {
        return (request, entity, context) -> {
            request.removeHeaders(HttpHeaders.CONTENT_TYPE);
            request.removeHeaders(HttpHeaders.ACCEPT);
            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            request.setHeader(HttpHeaders.ACCEPT, "application/json");
        };
    }

    private static String parseHostAndPort(String uris) {
        if (uris == null || uris.isBlank()) {
            return "localhost:9200";
        }
        try {
            URI uri = URI.create(uris.trim().split(",")[0]);
            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            int port = uri.getPort() > 0 ? uri.getPort() : 9200;
            return host + ":" + port;
        } catch (Exception e) {
            return "localhost:9200";
        }
    }
}
