package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.ai.AiModelRouter;
import org.rooms.roombuddy.ai.rag.AiRagRepository;
import org.rooms.roombuddy.dto.response.AiIngestResponse;
import org.rooms.roombuddy.exception.BadRequestException;
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

    @Value("${AI_DOCS_DIR:../docs}")
    private String docsDir;

    /**
     * Ingests all markdown docs from docsDir into pgvector.
     */
    public AiIngestResponse ingestDocs(boolean force) {
        Path dir = Path.of(docsDir);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new BadRequestException("Docs directory not found: " + dir.toAbsolutePath());
        }

        List<Path> files;
        try {
            files = Files.list(dir)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new BadRequestException("Failed to read docs directory");
        }

        int docsProcessed = 0;
        int chunksInserted = 0;
        boolean skippedUnchanged = false;

        for (Path p : files) {
            String source = "docs/" + p.getFileName();
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

            List<String> chunks = chunkMarkdown(text);
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                List<Double> emb = modelRouter.embed(chunkText);
                Map<String, Object> metadata = Map.of(
                        "source", source,
                        "title", title,
                        "chunkIndex", i
                );
                ragRepository.insertChunk(docId, i, chunkText, emb, metadata);
                chunksInserted++;
            }
            docsProcessed++;
        }

        return AiIngestResponse.builder()
                .documentsProcessed(docsProcessed)
                .chunksInserted(chunksInserted)
                .skippedUnchanged(skippedUnchanged)
                .build();
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

