package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.MatchActionRequest;
import org.rooms.roombay.dto.response.MatchResponse;
import org.rooms.roombay.entity.Match;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.MatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Matching", description = "APIs for roommate matching")
public class MatchController {
    
    private final MatchingService matchingService;
    
    @PostMapping("/find")
    @Operation(summary = "Find matches", description = "Find compatible roommate matches for a user")
    public ResponseEntity<List<MatchResponse>> findMatches() {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Finding matches for user: {}", userId);
        List<MatchResponse> matches = matchingService.findMatches(userId);
        return ResponseEntity.ok(matches);
    }

    @PostMapping("/find/{ignoredUserId}")
    public ResponseEntity<List<MatchResponse>> findMatchesLegacy(@PathVariable UUID ignoredUserId) {
        return findMatches();
    }
    
    @GetMapping
    @Operation(summary = "Get matches", description = "Get all matches for a user with optional status filter")
    public ResponseEntity<List<MatchResponse>> getMatches(
            @RequestParam(required = false) String status) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Getting matches for user: {} with status: {}", userId, status);
        
        Match.Status matchStatus = null;
        if (status != null) {
            try {
                matchStatus = Match.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        List<MatchResponse> matches = matchingService.getMatches(userId, matchStatus);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{ignoredUserId}")
    public ResponseEntity<List<MatchResponse>> getMatchesLegacy(
            @PathVariable UUID ignoredUserId,
            @RequestParam(required = false) String status) {
        return getMatches(status);
    }
    
    @GetMapping("/pending")
    @Operation(summary = "Get pending matches", description = "Get pending matches for a user")
    public ResponseEntity<List<MatchResponse>> getPendingMatches() {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Getting pending matches for user: {}", userId);
        List<MatchResponse> matches = matchingService.getPendingMatches(userId);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{ignoredUserId}/pending")
    public ResponseEntity<List<MatchResponse>> getPendingMatchesLegacy(@PathVariable UUID ignoredUserId) {
        return getPendingMatches();
    }
    
    @PostMapping("/{matchId}/action")
    @Operation(summary = "Accept or reject match", description = "Accept or reject a match request")
    public ResponseEntity<MatchResponse> acceptOrRejectMatch(
            @PathVariable UUID matchId,
            @Valid @RequestBody MatchActionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} performing action {} on match: {}", userId, request.getAction(), matchId);
        MatchResponse response = matchingService.acceptOrRejectMatch(userId, matchId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/action/{ignoredUserId}")
    public ResponseEntity<MatchResponse> acceptOrRejectMatchLegacy(
            @PathVariable UUID matchId,
            @PathVariable UUID ignoredUserId,
            @Valid @RequestBody MatchActionRequest request) {
        return acceptOrRejectMatch(matchId, request);
    }
    
    @GetMapping("/recommended")
    @Operation(summary = "Get recommended matches", description = "Get top recommended matches for a user")
    public ResponseEntity<List<MatchResponse>> getRecommendedMatches(
            @RequestParam(defaultValue = "10") int limit) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Getting recommended matches for user: {} (limit: {})", userId, limit);
        List<MatchResponse> matches = matchingService.getRecommendedMatches(userId, limit);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{ignoredUserId}/recommended")
    public ResponseEntity<List<MatchResponse>> getRecommendedMatchesLegacy(
            @PathVariable UUID ignoredUserId,
            @RequestParam(defaultValue = "10") int limit) {
        return getRecommendedMatches(limit);
    }
}

