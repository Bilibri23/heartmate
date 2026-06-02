package org.rooms.roombay.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.ai.AiModelRouter;
import org.rooms.roombay.ai.rag.AiGraphRagIngestLinker;
import org.rooms.roombay.ai.rag.AiRagRepository;
import org.rooms.roombay.dto.response.AiIngestResponse;
import org.rooms.roombay.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiIngestionService {

    private final AiModelRouter modelRouter;
    private final AiRagRepository ragRepository;
    private final AiGraphRagIngestLinker graphRagIngestLinker;

    @Value("${AI_DOCS_DIR:../docs}")
    private String docsDir;

    @PostConstruct
    void logDocsDiagnostic() {
        Path configured = Path.of(docsDir).toAbsolutePath().normalize();
        Path production = Path.of("/app/docs").toAbsolutePath().normalize();
        log.info(
                "[AI] Docs diagnostic: AI_DOCS_DIR='{}', configuredPath={}, configuredExists={}, configuredMarkdownFiles={}, productionPath={}, productionExists={}, productionMarkdownFiles={}",
                docsDir,
                configured,
                Files.isDirectory(configured),
                countMarkdownFiles(configured),
                production,
                Files.isDirectory(production),
                countMarkdownFiles(production)
        );
    }

    /**
     * Ingests all markdown docs from docsDir into pgvector.
     */
    public AiIngestResponse ingestDocs(boolean force) {
        Path dir = resolveDocsDir();
        log.info("[AI] Ingesting docs from {} (markdownFiles={})", dir, countMarkdownFiles(dir));

        List<Path> files;
        try {
            files = Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new BadRequestException("Failed to read docs directory");
        }

        int docsProcessed = 0;
        int chunksInserted = 0;
        int graphEntitiesWritten = 0;
        int graphEdgesWritten = 0;
        boolean skippedUnchanged = false;

        for (Path p : files) {
            String relative = dir.relativize(p).toString().replace('\\', '/');
            String source = "docs/" + relative;
            String text;
            try {
                text = Files.readString(p);
            } catch (IOException e) {
                log.warn("[AI] Failed reading {}", p);
                continue;
            }

            String checksum = AiRagRepository.sha256(text);
            var existing = ragRepository.findDocumentBySource(source);
            if (!force && existing.isPresent() && checksum.equals(existing.get().getChecksum())) {
                skippedUnchanged = true;
                continue;
            }

            String title = extractTitle(text, p.getFileName().toString());
            UUID docId = ragRepository.upsertDocument(source, title, checksum);
            ragRepository.deleteChunksForDocument(docId);
            ragRepository.deleteEntitiesForDocument(docId);

            List<String> chunks = chunkMarkdown(text);
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                List<Double> emb = modelRouter.embed(chunkText);
                Map<String, Object> metadata = Map.of(
                        "source", source,
                        "title", title,
                        "chunkIndex", i
                );
                UUID chunkId = ragRepository.insertChunk(docId, i, chunkText, emb, metadata);
                chunksInserted++;
                try {
                    AiGraphRagIngestLinker.Stats gs = graphRagIngestLinker.linkChunk(docId, source, chunkId, chunkText);
                    graphEntitiesWritten += gs.entities();
                    graphEdgesWritten += gs.edges();
                } catch (Exception ex) {
                    log.warn("[AI] Graph ingest failed for chunk {}: {}", chunkId, ex.getMessage());
                }
            }
            docsProcessed++;
        }

        return AiIngestResponse.builder()
                .documentsProcessed(docsProcessed)
                .chunksInserted(chunksInserted)
                .skippedUnchanged(skippedUnchanged)
                .graphEntitiesWritten(graphEntitiesWritten)
                .graphEdgesWritten(graphEdgesWritten)
                .build();
    }

    /**
     * {@code AI_DOCS_DIR} is often {@code ../docs} when the JVM cwd is {@code backend/}, but IDEs usually
     * use the repo root, where {@code docs/} sits next to {@code backend/}. If the configured path is missing,
     * fall back to {@code ./docs} relative to {@code user.dir}.
     */
    private Path resolveDocsDir() {
        Path primary = Path.of(docsDir).toAbsolutePath().normalize();
        if (Files.isDirectory(primary)) {
            return primary;
        }
        Path fallback = Path.of("docs").toAbsolutePath().normalize();
        if (Files.isDirectory(fallback)) {
            log.warn("[AI] AI_DOCS_DIR not found at {}, using {}", primary, fallback);
            return fallback;
        }
        throw new BadRequestException("Docs directory not found. Set AI_DOCS_DIR (tried " + primary + " and " + fallback + ")");
    }

    private long countMarkdownFiles(Path dir) {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (var stream = Files.walk(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                    .count();
        } catch (IOException e) {
            log.warn("[AI] Failed counting markdown docs in {}: {}", dir, e.getMessage());
            return 0;
        }
    }

    private static String extractTitle(String text, String fallback) {
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
        }
        return fallback.replace(".md", "");
    }

    /**
     * Simple, headings-aware markdown chunker.
     * Targets ~3k-5k chars per chunk with overlap.
     */
    static List<String> chunkMarkdown(String md) {
        List<String> lines = Arrays.asList(md.split("\n"));
        List<String> sections = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("#") && current.length() > 0) {
                sections.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(line).append("\n");
        }
        if (current.length() > 0) sections.add(current.toString().trim());

        int target = 4000;
        int overlap = 500;
        List<String> chunks = new ArrayList<>();
        for (String sec : sections) {
            if (sec.length() <= target) {
                chunks.add(sec);
                continue;
            }
            int start = 0;
            while (start < sec.length()) {
                int end = Math.min(sec.length(), start + target);
                String chunk = sec.substring(start, end);
                chunks.add(chunk);
                if (end == sec.length()) break;
                start = Math.max(0, end - overlap);
            }
        }

        // remove tiny chunks
        return chunks.stream()
                .map(String::trim)
                .filter(s -> s.length() >= 200)
                .collect(Collectors.toList());
    }
}
