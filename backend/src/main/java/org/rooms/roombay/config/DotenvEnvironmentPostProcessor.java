package org.rooms.roombay.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

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
    }
}
