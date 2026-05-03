package org.rooms.roombay.ai.safety;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.ai.rag.AiRagRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Layer 1 of the safety stack: role-based filtering of RAG retrievals.
 *
 * <p>Today RoomBay's RAG corpus is built only from {@code docs/*.md} (public help content),
 * so this filter is conservative by default: drop chunks whose source path is flagged as
 * {@code admin-only} unless the caller has the ADMIN role. Centralizing the rule means
 * future private corpora (lease text, admin runbooks) can be onboarded with one allow-list change.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiRetrievalPolicy {

    private static final Set<String> ADMIN_ONLY_SOURCE_PREFIXES = Set.of(
            "docs/admin/",
            "docs/internal/",
            "docs/security/"
    );

    public List<AiRagRepository.ChunkRow> filter(List<AiRagRepository.ChunkRow> chunks, String userRole) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        if (isAdmin) {
            return chunks;
        }
        return chunks.stream()
                .filter(c -> !isAdminOnly(c.getSource()))
                .toList();
    }

    private static boolean isAdminOnly(String source) {
        if (source == null) {
            return false;
        }
        String s = source.toLowerCase(Locale.ROOT).replace('\\', '/');
        for (String prefix : ADMIN_ONLY_SOURCE_PREFIXES) {
            if (s.startsWith(prefix) || s.contains("/" + prefix)) {
                return true;
            }
        }
        return false;
    }
}
