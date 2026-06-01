package org.rooms.roombay.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads a local {@code .env} before {@code application.properties} resolution, without relying on
 * a non-existent Maven artifact. Tries {@code backend/.env} then {@code .env} relative to the JVM
 * working directory (IDE runs from repo root → {@code backend/.env} is found).
 * <p>
 * OS environment variables and system properties still override these values (standard 12-factor).
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenvFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String userDir = System.getProperty("user.dir", ".");
        Path[] candidates = new Path[] {
                Path.of(userDir, "backend", ".env"),
                Path.of(userDir, ".env")
        };
        Path envFile = null;
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                envFile = p;
                break;
            }
        }
        if (envFile == null) {
            addNormalizedDatabaseUrl(environment, Map.of());
            return;
        }

        Dotenv dotenv = Dotenv.configure()
                .directory(envFile.getParent().toString())
                .filename(envFile.getFileName().toString())
                .ignoreIfMalformed()
                .load();

        Map<String, Object> map = new HashMap<>();
        for (DotenvEntry e : dotenv.entries()) {
            map.put(e.getKey(), e.getValue());
        }

        MapPropertySource ps = new MapPropertySource(PROPERTY_SOURCE_NAME + "[" + envFile + "]", map);
        if (environment.getPropertySources().contains("systemEnvironment")) {
            environment.getPropertySources().addAfter("systemEnvironment", ps);
        } else {
            environment.getPropertySources().addFirst(ps);
        }

        addNormalizedDatabaseUrl(environment, map);
    }

    private void addNormalizedDatabaseUrl(ConfigurableEnvironment environment, Map<String, Object> dotenvValues) {
        if (environment.getProperty("spring.datasource.url") != null
                || environment.getProperty("SPRING_DATASOURCE_URL") != null) {
            return;
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if ((databaseUrl == null || databaseUrl.isBlank()) && dotenvValues.get("DATABASE_URL") instanceof String value) {
            databaseUrl = value;
        }
        if (databaseUrl == null || databaseUrl.isBlank() || databaseUrl.startsWith("jdbc:")) {
            return;
        }

        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            return;
        }

        Map<String, Object> normalized = normalizePostgresDatabaseUrl(databaseUrl);
        if (normalized.isEmpty()) {
            return;
        }

        MapPropertySource ps = new MapPropertySource("normalizedDatabaseUrl", normalized);
        environment.getPropertySources().addFirst(ps);
    }

    private Map<String, Object> normalizePostgresDatabaseUrl(String databaseUrl) {
        try {
            URI uri = URI.create(databaseUrl);
            String query = uri.getRawQuery();
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                    + (uri.getRawPath() != null ? uri.getRawPath() : "")
                    + (query != null && !query.isBlank() ? "?" + query : "");

            Map<String, Object> normalized = new HashMap<>();
            normalized.put("spring.datasource.url", jdbcUrl);

            String userInfo = uri.getRawUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                normalized.put("spring.datasource.username", decodeUrlPart(parts[0]));
                if (parts.length > 1) {
                    normalized.put("spring.datasource.password", decodeUrlPart(parts[1]));
                }
            }
            return normalized;
        } catch (IllegalArgumentException e) {
            return Map.of();
        }
    }

    private static String decodeUrlPart(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
