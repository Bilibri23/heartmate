package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.HomeFeedResponse;
import org.rooms.roombay.service.FeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Compound discovery feed: single response for for-you, trending, recent, and optional reels.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feed", description = "Home discovery feed (compound sections)")
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/feed")
    @Operation(summary = "Home feed", description = "Returns requested sections in one response. Personalizes for-you when authenticated.")
    public ResponseEntity<HomeFeedResponse> getFeed(
            @RequestParam(defaultValue = "forYou,trending,recent") String sections,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "en") String lang,
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = null;
        if (principal != null && principal.getUsername() != null) {
            try {
                userId = UUID.fromString(principal.getUsername());
            } catch (IllegalArgumentException e) {
                log.warn("Feed: could not parse user id from principal");
            }
        }

        HomeFeedResponse response = feedService.buildFeed(sections, size, lang, userId);
        return ResponseEntity.ok(response);
    }
}
