package org.rooms.roombay.ai.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic extraction of entities and edges from markdown chunks for GraphRAG.
 * Uses headings, markdown links to other docs, and a small domain keyword set.
 */
public final class AiMarkdownGraphExtractor {

    private static final Pattern MD_LINK = Pattern.compile("\\[([^\\]]*)]\\(([^)]+)\\)");
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");

    /** Domain terms that often co-occur across RoomBay docs (lowercase). */
    private static final String[] DOMAIN_KEYWORDS = {
            "roombay", "listing", "landlord", "tenant", "lease", "verification",
            "application", "deposit", "feed", "discovery", "search", "favorites"
    };

    private AiMarkdownGraphExtractor() {
    }

    public record EntitySpec(String canonicalKey, String type, String label, float weight) {
    }

    public record EdgeSpec(String srcKey, String dstKey, String relationType, float weight) {
    }

    public record ExtractedGraph(List<EntitySpec> entities, List<EdgeSpec> edges) {
    }

    public static ExtractedGraph extract(String chunkText, String source) {
        List<EntitySpec> entities = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();

        String[] lines = chunkText.split("\\R");
        String firstHeadingKey = null;

        for (String line : lines) {
            Matcher hm = HEADING.matcher(line.trim());
            if (hm.matches()) {
                String title = hm.group(2).trim();
                String key = canonicalHeading(source, title);
                if (seenKeys.add(key)) {
                    entities.add(new EntitySpec(key, "HEADING", title, 1.0f));
                    if (firstHeadingKey == null) {
                        firstHeadingKey = key;
                    }
                }
            }
        }

        Matcher lm = MD_LINK.matcher(chunkText);
        while (lm.find()) {
            String target = lm.group(2).trim();
            if (!target.toLowerCase(Locale.ROOT).endsWith(".md")) {
                continue;
            }
            String normalized = normalizeDocPath(target, source);
            String key = "doc:" + normalized;
            if (seenKeys.add(key)) {
                entities.add(new EntitySpec(key, "DOC_REF", normalized, 0.9f));
            }
            if (firstHeadingKey != null) {
                // Structural link from section to referenced doc
                // (handled again in edges list below)
            }
        }

        String lower = chunkText.toLowerCase(Locale.ROOT);
        for (String kw : DOMAIN_KEYWORDS) {
            if (lower.contains(kw)) {
                String key = canonicalTopic(source, kw);
                if (seenKeys.add(key)) {
                    entities.add(new EntitySpec(key, "TOPIC", kw, 0.6f));
                }
            }
        }

        List<EdgeSpec> edges = new ArrayList<>();
        List<String> keys = entities.stream().map(EntitySpec::canonicalKey).toList();
        int cap = Math.min(keys.size(), 24);
        for (int i = 0; i < cap; i++) {
            for (int j = i + 1; j < cap; j++) {
                edges.add(new EdgeSpec(keys.get(i), keys.get(j), "CO_OCCURS", 0.4f));
            }
        }

        if (firstHeadingKey != null) {
            for (EntitySpec e : entities) {
                if ("DOC_REF".equals(e.type())) {
                    edges.add(new EdgeSpec(firstHeadingKey, e.canonicalKey(), "REFERENCES", 0.85f));
                }
            }
        }

        return new ExtractedGraph(entities, edges);
    }

    private static String canonicalHeading(String source, String title) {
        return source + "#h:" + slug(title);
    }

    private static String canonicalTopic(String source, String keyword) {
        return source + "#topic:" + slug(keyword);
    }

    static String slug(String raw) {
        String s = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s.isEmpty() ? "x" : s;
    }

    /**
     * Normalize relative links like ./foo.md or ../docs/bar.md to docs/... style when possible.
     */
    static String normalizeDocPath(String target, String currentSource) {
        String t = target.replace('\\', '/').trim();
        if (t.startsWith("http://") || t.startsWith("https://")) {
            int i = t.lastIndexOf('/');
            return t.substring(i + 1);
        }
        if (t.startsWith("./")) {
            t = t.substring(2);
        }
        if (!t.startsWith("docs/") && t.endsWith(".md")) {
            return "docs/" + t;
        }
        return t;
    }
}
