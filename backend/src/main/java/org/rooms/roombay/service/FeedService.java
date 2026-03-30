package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.FeedSectionResponse;
import org.rooms.roombay.dto.response.HomeFeedResponse;
import org.rooms.roombay.dto.response.ListingResponse;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.search.ElasticsearchSearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Composes discovery sections for the home feed (for-you, trending, recent, reels)
 * without splitting the monolith — delegates to RecommendationService, Elasticsearch, and ListingService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FeedService {

    private final RecommendationService recommendationService;
    private final Optional<ElasticsearchSearchService> elasticsearchSearchService;
    private final ListingService listingService;
    private final PropertyListingRepository listingRepository;

    public HomeFeedResponse buildFeed(String sectionsParam, int size, String lang, UUID userId) {
        Set<String> wanted = parseSections(sectionsParam);
        HomeFeedResponse.HomeFeedResponseBuilder b = HomeFeedResponse.builder();
        b.videoTourListingCount(listingRepository.countActiveWithVideoTour());

        if (wanted.contains("foryou")) {
            b.forYou(buildForYou(userId, size, lang));
        }
        if (wanted.contains("trending")) {
            b.trending(buildTrending(userId, size, lang));
        }
        if (wanted.contains("recent")) {
            b.recent(buildRecent(userId, size, lang));
        }
        if (wanted.contains("reels")) {
            b.reels(buildReels(userId, size));
        }

        return b.build();
    }

    private static Set<String> parseSections(String sectionsParam) {
        if (sectionsParam == null || sectionsParam.isBlank()) {
            return Set.of("foryou", "trending", "recent");
        }
        Set<String> out = new HashSet<>();
        for (String part : sectionsParam.split(",")) {
            String s = part.trim().toLowerCase(Locale.ROOT);
            if (s.equals("foryou") || s.equals("for-you")) {
                out.add("foryou");
            } else if (s.equals("trending")) {
                out.add("trending");
            } else if (s.equals("recent")) {
                out.add("recent");
            } else if (s.equals("reels")) {
                out.add("reels");
            }
        }
        if (out.isEmpty()) {
            return Set.of("foryou", "trending", "recent");
        }
        return out;
    }

    private FeedSectionResponse buildForYou(UUID userId, int size, String lang) {
        if (userId != null) {
            List<RecommendationService.ScoredListing> recs = recommendationService.getRecommendedListings(userId);
            if (!recs.isEmpty()) {
                List<ListingResponse> items = recs.stream()
                        .limit(size)
                        .map(s -> listingService.toListingResponse(s.getListing(), userId))
                        .collect(Collectors.toList());
                return FeedSectionResponse.builder()
                        .items(items)
                        .total(recs.size())
                        .build();
            }
        }

        Page<ListingResponse> page = searchMode(ElasticsearchSearchService.MODE_FOR_YOU, userId, size, lang);
        return FeedSectionResponse.builder()
                .items(page.getContent())
                .total(page.getTotalElements())
                .build();
    }

    private FeedSectionResponse buildTrending(UUID userId, int size, String lang) {
        Page<ListingResponse> page = searchMode(ElasticsearchSearchService.MODE_TRENDING, userId, size, lang);
        return FeedSectionResponse.builder()
                .items(page.getContent())
                .total(page.getTotalElements())
                .build();
    }

    private FeedSectionResponse buildRecent(UUID userId, int size, String lang) {
        Page<ListingResponse> page = searchMode(ElasticsearchSearchService.MODE_RECENT, userId, size, lang);
        return FeedSectionResponse.builder()
                .items(page.getContent())
                .total(page.getTotalElements())
                .build();
    }

    private FeedSectionResponse buildReels(UUID userId, int size) {
        Pageable pageable = PageRequest.of(0, size);
        Page<PropertyListing> page = listingRepository.findActiveWithVideoTour(pageable);
        List<ListingResponse> items = page.getContent().stream()
                .map(l -> listingService.toListingResponse(l, userId))
                .collect(Collectors.toList());
        return FeedSectionResponse.builder()
                .items(items)
                .total(page.getTotalElements())
                .build();
    }

    /**
     * Mirrors SearchController: ES when available; DB fallback when ES returns empty or is disabled.
     */
    private Page<ListingResponse> searchMode(String mode, UUID userId, int size, String lang) {
        Pageable pageable = PageRequest.of(0, size, Sort.by("createdAt").descending());
        if (elasticsearchSearchService.isPresent()) {
            try {
                Page<ListingResponse> results = elasticsearchSearchService.get().search(
                        null, null, null, null, null, null, null, null, null,
                        null, null, null,
                        lang != null ? lang : "en",
                        mode,
                        userId,
                        pageable);
                if (!results.isEmpty() || results.getTotalElements() > 0) {
                    return results;
                }
                log.info("Elasticsearch returned empty for feed mode {} — using DB fallback", mode);
            } catch (RuntimeException e) {
                log.warn("Elasticsearch feed mode {} failed: {}", mode, e.getMessage());
            }

            Pageable fallbackPageable = pageable;
            if (ElasticsearchSearchService.MODE_RECENT.equalsIgnoreCase(mode)) {
                fallbackPageable = PageRequest.of(0, size, Sort.by("createdAt").descending());
            } else if (ElasticsearchSearchService.MODE_TRENDING.equalsIgnoreCase(mode)) {
                fallbackPageable = PageRequest.of(0, size,
                        Sort.by("viewsCount").descending().and(Sort.by("createdAt").descending()));
            }
            return listingService.searchListingsAdvanced(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null,
                    null, userId, fallbackPageable);
        }

        return dbFallbackForMode(mode, userId, size);
    }

    private Page<ListingResponse> dbFallbackForMode(String mode, UUID userId, int size) {
        Pageable pageable = PageRequest.of(0, size);
        if (ElasticsearchSearchService.MODE_TRENDING.equalsIgnoreCase(mode)) {
            Page<PropertyListing> page = listingRepository.findActiveOrderByTrending(pageable);
            return page.map(l -> listingService.toListingResponse(l, userId));
        }
        if (ElasticsearchSearchService.MODE_RECENT.equalsIgnoreCase(mode)) {
            Page<PropertyListing> page = listingRepository.findActiveOrderByRecent(pageable);
            return page.map(l -> listingService.toListingResponse(l, userId));
        }
        // forYou (and relevance): fresh inventory when ES is off
        Page<PropertyListing> page = listingRepository.findActiveOrderByRecent(pageable);
        return page.map(l -> listingService.toListingResponse(l, userId));
    }
}
