package org.rooms.roombay.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
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

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required non-dev configuration values: " + String.join(", ", missing));
        }

        String jwt = environment.getProperty("spring.jwt.secret");
        if (StringUtils.hasText(jwt) && jwt.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes (256 bits) for HS256.");
        }
    }

    private void require(String property, String envKey, List<String> missing) {
        String value = environment.getProperty(property);
        if (!StringUtils.hasText(value)) {
            missing.add(envKey);
        }
    }
}
