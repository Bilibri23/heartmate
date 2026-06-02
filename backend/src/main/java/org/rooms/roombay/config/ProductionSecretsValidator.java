package org.rooms.roombay.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionSecretsValidator {
    private final Environment environment;

    @PostConstruct
    public void validate() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isNonDev = activeProfiles.stream().anyMatch(p -> !"dev".equalsIgnoreCase(p));
        if (!isNonDev) {
            return;
        }

        List<String> missing = new ArrayList<>();
        require("spring.jwt.secret", "JWT_SECRET", missing);
        require("cloudinary.cloud-name", "CLOUDINARY_CLOUD_NAME", missing);
        require("cloudinary.api-key", "CLOUDINARY_API_KEY", missing);
        require("cloudinary.api-secret", "CLOUDINARY_API_SECRET", missing);
        require("cors.allowed-origins", "CORS_ALLOWED_ORIGINS", missing);
        require("spring.datasource.url", "SPRING_DATASOURCE_URL or DATABASE_URL", missing);
        require("spring.mail.username", "MAIL_USERNAME", missing);
        require("spring.mail.password", "MAIL_PASSWORD", missing);

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required non-dev configuration values: " + String.join(", ", missing));
        }

        String jwt = environment.getProperty("spring.jwt.secret");
        if (StringUtils.hasText(jwt) && jwt.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes (256 bits) for HS256.");
        }

        rejectUnresolvedRailwayTemplate("spring.datasource.username", "SPRING_DATASOURCE_USERNAME");
        rejectUnresolvedRailwayTemplate("spring.datasource.password", "SPRING_DATASOURCE_PASSWORD");
        rejectUnresolvedRailwayTemplate("spring.flyway.user", "SPRING_FLYWAY_USER");
        rejectUnresolvedRailwayTemplate("spring.flyway.password", "SPRING_FLYWAY_PASSWORD");

        boolean prod = activeProfiles.stream().anyMatch("prod"::equalsIgnoreCase);
        boolean adminSeedEnabled = Boolean.parseBoolean(environment.getProperty("app.admin.seed.enabled", "false"));
        if (prod && adminSeedEnabled) {
            throw new IllegalStateException("APP_ADMIN_SEED_ENABLED must be false in prod. Create the admin once, then disable the seed before deploying.");
        }
        if (adminSeedEnabled) {
            log.warn("APP_ADMIN_SEED_ENABLED is true in a non-dev profile. Disable it immediately after the admin exists.");
        }

        boolean redisEnabled = Boolean.parseBoolean(environment.getProperty("spring.data.redis.enabled", "false"));
        boolean horizontalScaling = Boolean.parseBoolean(environment.getProperty("roombay.production.horizontal-scaling-enabled", "false"));
        boolean redisRequired = Boolean.parseBoolean(environment.getProperty("ratelimit.redis.required", String.valueOf(prod && horizontalScaling)));
        if (prod && (redisEnabled || horizontalScaling || redisRequired)) {
            requireRedisProductionConfig();
        }
    }

    private void require(String property, String envKey, List<String> missing) {
        String value = environment.getProperty(property);
        if (!StringUtils.hasText(value)) {
            missing.add(envKey);
        }
    }

    private void rejectUnresolvedRailwayTemplate(String property, String envKey) {
        String value = environment.getProperty(property);
        if (value != null && value.contains("${{")) {
            throw new IllegalStateException(envKey + " still contains an unresolved Railway template. Link the Postgres service variables or set the resolved value directly.");
        }
    }

    private void requireRedisProductionConfig() {
        String host = environment.getProperty("spring.data.redis.host");
        if (!StringUtils.hasText(host) || "localhost".equalsIgnoreCase(host.trim()) || "127.0.0.1".equals(host.trim())) {
            throw new IllegalStateException("Production Redis rate limiting requires REDIS_HOST to point at the managed Redis service, not localhost.");
        }
    }
}
