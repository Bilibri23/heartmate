package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AiIngestionServiceTest {

    @Test
    void resolvesConfiguredDocsDirectory() throws Exception {
        Path docs = Files.createTempDirectory("roombay-ai-docs");
        AiIngestionService service = new AiIngestionService(null, null, null);
        ReflectionTestUtils.setField(service, "docsDir", docs.toString());

        assertThat(service.resolveDocsDir()).isEqualTo(docs.toAbsolutePath().normalize());
    }

    @Test
    void fallsBackToRepoDocsDirectoryWhenConfiguredPathIsMissing() {
        AiIngestionService service = new AiIngestionService(null, null, null);
        ReflectionTestUtils.setField(service, "docsDir", "__missing_ai_docs_dir__");

        assertThat(service.resolveDocsDir().getFileName().toString()).isEqualTo("docs");
    }

    @Test
    void packagedAiDocsAreAvailableOnClasspath() throws Exception {
        Resource[] docs = new PathMatchingResourcePatternResolver().getResources("classpath*:/ai-docs/**/*.md");

        assertThat(Arrays.stream(docs).filter(Resource::isReadable)).hasSizeGreaterThanOrEqualTo(30);
    }
}
