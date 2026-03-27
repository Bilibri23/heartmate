package org.rooms.roombuddy.config;

import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads {@code backend/.env} when the working directory is the repo root (Maven {@code user.dir}).
 */
public final class BackendEnvFileSupport {

    private BackendEnvFileSupport() {
    }

    public static Optional<Path> locateEnvFile() {
        String userDir = System.getProperty("user.dir", ".");
        Path[] candidates = {
                Path.of(userDir, "backend", ".env"),
                Path.of(userDir, ".env"),
        };
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                return Optional.of(p.toAbsolutePath().normalize());
            }
        }
        return Optional.empty();
    }

    public static Map<String, Object> parseEnvFile(Path envFile) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            if ("MAIL_PASSWORD".equals(key)) {
                value = value.replaceAll("\\s+", "");
            }
            if ("APP_FRONTEND_URL".equals(key) || "APP_BASE_URL".equals(key)) {
                value = value.trim();
            }
            map.put(key, value);
        }
        return map;
    }

    /**
     * Last-resort read for keys that are blank in the Spring Environment (e.g. {@code JWT_SECRET=""}
     * in OS env blocks {@code backend/.env} from lower-priority property sources).
     */
    public static Optional<String> readProperty(String key) {
        Optional<Path> path = locateEnvFile();
        if (path.isEmpty()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> map = parseEnvFile(path.get());
            Object v = map.get(key);
            if (v == null) {
                return Optional.empty();
            }
            String s = String.valueOf(v).trim();
            return StringUtils.hasText(s) ? Optional.of(s) : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
