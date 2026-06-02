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
 * <p>RoomBay's RAG corpus is organized by audience. Public docs can be used for every role,
 * tenant docs are tenant-only, landlord docs are landlord-only, and admin/internal/security
 * docs are admin-only.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiRetrievalPolicy {

    private static final Set<String> PUBLIC_SOURCE_PREFIXES = Set.of(
            "docs/public/"
    );

    private static final Set<String> TENANT_SOURCE_PREFIXES = Set.of(
            "docs/tenant/"
    );

    private static final Set<String> LANDLORD_SOURCE_PREFIXES = Set.of(
            "docs/landlord/"
    );

    private static final Set<String> ADMIN_SOURCE_PREFIXES = Set.of(
            "docs/admin/",
            "docs/internal/",
            "docs/security/"
    );

    public List<AiRagRepository.ChunkRow> filter(List<AiRagRepository.ChunkRow> chunks, String userRole) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        String role = userRole == null ? "" : userRole.toUpperCase(Locale.ROOT);
        if ("ADMIN".equals(role)) {
            return chunks;
        }
        return chunks.stream()
                .filter(c -> isAllowedForRole(c.getSource(), role))
                .toList();
    }

    private static boolean isAllowedForRole(String source, String role) {
        if (source == null) {
            return false;
        }
        String s = source.toLowerCase(Locale.ROOT).replace('\\', '/');
        if (matchesAnyPrefix(s, PUBLIC_SOURCE_PREFIXES)) {
            return true;
        }
        if ("LANDLORD".equals(role)) {
            return matchesAnyPrefix(s, LANDLORD_SOURCE_PREFIXES);
        }
        return matchesAnyPrefix(s, TENANT_SOURCE_PREFIXES);
    }

    private static boolean matchesAnyPrefix(String normalizedSource, Set<String> prefixes) {
        for (String prefix : prefixes) {
            if (normalizedSource.startsWith(prefix) || normalizedSource.contains("/" + prefix)) return true;
        }
        return false;
    }
}
