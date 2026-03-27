package org.rooms.roombuddy.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads {@code backend/.env} into the Spring environment when the app is run from the repo root
 * (sources live under {@code backend/} but Maven {@code user.dir} is usually the repo root).
 * <p>
 * Uses {@link ConfigurableEnvironment#getProperty(String)} to decide which keys to apply: only keys
 * that are still missing or blank after command-line args / system env / system properties. That
 * fixes {@code JWT_SECRET} being set to an empty string in the OS (which would otherwise block
 * lower-priority sources). The resulting map is {@link org.springframework.core.env.MutablePropertySources#addFirst}
 * so non-blank values from this file override those empty entries.
 * <p>
 * Gmail app passwords are often copied with spaces; {@code MAIL_PASSWORD} is normalized in
 * {@link BackendEnvFileSupport#parseEnvFile(java.nio.file.Path)}.
 */
public class BackendDotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "backendDotenvFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        var path = BackendEnvFileSupport.locateEnvFile();
        if (path.isEmpty()) {
            return;
        }
        Map<String, Object> map;
        try {
            map = BackendEnvFileSupport.parseEnvFile(path.get());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read env file: " + path.get().toAbsolutePath(), e);
        }
        Map<String, Object> effective = filterOnlyMissingOrBlank(environment, map);
        if (effective.isEmpty()) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, effective));
    }

    /**
     * Skip keys that already have a non-blank value anywhere in the environment (e.g. IDE launch,
     * {@code -D}, or {@code --} args).
     */
    static Map<String, Object> filterOnlyMissingOrBlank(ConfigurableEnvironment environment, Map<String, Object> file) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : file.entrySet()) {
            String key = e.getKey();
            if (StringUtils.hasText(environment.getProperty(key))) {
                continue;
            }
            out.put(key, e.getValue());
        }
        return out;
    }
}
