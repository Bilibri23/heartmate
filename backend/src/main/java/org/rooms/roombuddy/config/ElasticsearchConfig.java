package org.rooms.roombay.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

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
        log.info("Elasticsearch configured: {}", hostPort);
        return ClientConfiguration.builder()
                .connectedTo(hostPort)
                .build();
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
