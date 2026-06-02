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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    private final AiIngestRunService aiIngestRunService;
    private final AppErrorLogService appErrorLogService;

    @Value("${AI_DOCS_DIR:../docs}")
    private String docsDir;

    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    @PostConstruct
    void logDocsDiagnostic() {
        Path configured = Path.of(docsDir).toAbsolutePath().normalize();
        Path production = Path.of("/app/docs").toAbsolutePath().normalize();
        log.info(
                "[AI] Docs diagnostic: AI_DOCS_DIR='{}', configuredPath={}, configuredExists={}, configuredMarkdownFiles={}, productionPath={}, productionExists={}, productionMarkdownFiles={}, classpathMarkdownFiles={}",
                docsDir,
                configured,
                Files.isDirectory(configured),
                countMarkdownFiles(configured),
                production,
                Files.isDirectory(production),
                countMarkdownFiles(production),
                countClasspathMarkdownFiles()
        );
    }

    /**
     * Ingests all markdown docs from docsDir into pgvector.
     */
    public AiIngestResponse ingestDocs(boolean force) {
        UUID runId = aiIngestRunService.start();
        try {
            AiIngestResponse response = ingestDocsInternal(force);
            aiIngestRunService.success(runId, response);
            return response;
        } catch (RuntimeException ex) {
            aiIngestRunService.failure(runId, ex);
            appErrorLogService.log("ERROR", "AI", "AI ingest failed: " + ex.getMessage(), "/api/ai/admin/ingest", ex);
            throw ex;
        }
    }

    private AiIngestResponse ingestDocsInternal(boolean force) {
        List<DocFile> files = loadDocs();

        int docsProcessed = 0;
        int chunksInserted = 0;
        int graphEntitiesWritten = 0;
        int graphEdgesWritten = 0;
        boolean skippedUnchanged = false;

        for (DocFile file : files) {
            String source = file.source();
            String text = file.text();

            String checksum = AiRagRepository.sha256(text);
            var existing = ragRepository.findDocumentBySource(source);
            if (!force && existing.isPresent() && checksum.equals(existing.get().getChecksum())) {
                skippedUnchanged = true;
                continue;
            }

            String title = extractTitle(text, file.fileName());
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
    Path resolveDocsDir() {
        return resolveFilesystemDocsDir()
                .orElseThrow(() -> new BadRequestException("Docs directory not found. Set AI_DOCS_DIR (tried "
                        + Path.of(docsDir).toAbsolutePath().normalize()
                        + " and "
                        + Path.of("docs").toAbsolutePath().normalize()
                        + ")"));
    }

    private List<DocFile> loadDocs() {
        Optional<Path> filesystemDocs = resolveFilesystemDocsDir();
        if (filesystemDocs.isPresent()) {
            Path dir = filesystemDocs.get();
            log.info("[AI] Ingesting docs from filesystem {} (markdownFiles={})", dir, countMarkdownFiles(dir));
            return loadFilesystemDocs(dir);
        }

        List<DocFile> classpathDocs = loadClasspathDocs();
        if (!classpathDocs.isEmpty()) {
            log.warn("[AI] AI_DOCS_DIR not found; ingesting {} packaged classpath docs from ai-docs/", classpathDocs.size());
            return classpathDocs;
        }

        throw new BadRequestException("Docs directory not found. Set AI_DOCS_DIR (tried "
                + Path.of(docsDir).toAbsolutePath().normalize()
                + " and "
                + Path.of("docs").toAbsolutePath().normalize()
                + "; classpath ai-docs also empty)");
    }

    private Optional<Path> resolveFilesystemDocsDir() {
        Path primary = Path.of(docsDir).toAbsolutePath().normalize();
        if (Files.isDirectory(primary)) return Optional.of(primary);

        Path fallback = Path.of("docs").toAbsolutePath().normalize();
        if (Files.isDirectory(fallback)) {
            log.warn("[AI] AI_DOCS_DIR not found at {}, using {}", primary, fallback);
            return Optional.of(fallback);
        }
        return Optional.empty();
    }

    private List<DocFile> loadFilesystemDocs(Path dir) {
        try (var stream = Files.walk(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                    .sorted()
                    .map(p -> readFilesystemDoc(dir, p))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new BadRequestException("Failed to read docs directory");
        }
    }

    private Optional<DocFile> readFilesystemDoc(Path dir, Path p) {
        try {
            String relative = dir.relativize(p).toString().replace('\\', '/');
            return Optional.of(new DocFile("docs/" + relative, p.getFileName().toString(), Files.readString(p)));
        } catch (IOException e) {
            log.warn("[AI] Failed reading {}", p);
            return Optional.empty();
        }
    }

    private List<DocFile> loadClasspathDocs() {
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath*:/ai-docs/**/*.md");
            List<DocFile> docs = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.isReadable()) continue;
                String relative = classpathDocRelativePath(resource);
                try (InputStream input = resource.getInputStream()) {
                    String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    docs.add(new DocFile("docs/" + relative, Objects.requireNonNullElse(resource.getFilename(), "unknown.md"), text));
                }
            }
            docs.sort(Comparator.comparing(DocFile::source));
            return docs;
        } catch (IOException e) {
            log.warn("[AI] Failed reading classpath ai-docs resources: {}", e.getMessage());
            return List.of();
        }
    }

    private String classpathDocRelativePath(Resource resource) throws IOException {
        String raw = URLDecoder.decode(resource.getURL().toString(), StandardCharsets.UTF_8);
        int marker = raw.indexOf("ai-docs/");
        if (marker >= 0) {
            return raw.substring(marker + "ai-docs/".length()).replace('\\', '/');
        }
        return Objects.requireNonNullElse(resource.getFilename(), "unknown.md");
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

    private long countClasspathMarkdownFiles() {
        try {
            return Arrays.stream(resourcePatternResolver.getResources("classpath*:/ai-docs/**/*.md"))
                    .filter(Resource::isReadable)
                    .count();
        } catch (IOException e) {
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

    private record DocFile(String source, String fileName, String text) {
    }
}
