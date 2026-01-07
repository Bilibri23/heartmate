package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.MatchActionRequest;
import org.rooms.roombuddy.dto.response.MatchResponse;
import org.rooms.roombuddy.entity.Match;
import org.rooms.roombuddy.service.MatchingService;
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
    public ResponseEntity<List<MatchResponse>> findMatches(@RequestParam UUID userId) {
        log.info("Finding matches for user: {}", userId);
        List<MatchResponse> matches = matchingService.findMatches(userId);
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get matches", description = "Get all matches for a user with optional status filter")
    public ResponseEntity<List<MatchResponse>> getMatches(
            @PathVariable UUID userId,
            @RequestParam(required = false) String status) {
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
    
    @GetMapping("/{userId}/pending")
    @Operation(summary = "Get pending matches", description = "Get pending matches for a user")
    public ResponseEntity<List<MatchResponse>> getPendingMatches(@PathVariable UUID userId) {
        log.info("Getting pending matches for user: {}", userId);
        List<MatchResponse> matches = matchingService.getPendingMatches(userId);
        return ResponseEntity.ok(matches);
    }
    
    @PostMapping("/{matchId}/action")
    @Operation(summary = "Accept or reject match", description = "Accept or reject a match request")
    public ResponseEntity<MatchResponse> acceptOrRejectMatch(
            @PathVariable UUID matchId,
            @RequestParam UUID userId,
            @Valid @RequestBody MatchActionRequest request) {
        log.info("User {} performing action {} on match: {}", userId, request.getAction(), matchId);
        MatchResponse response = matchingService.acceptOrRejectMatch(userId, matchId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{userId}/recommended")
    @Operation(summary = "Get recommended matches", description = "Get top recommended matches for a user")
    public ResponseEntity<List<MatchResponse>> getRecommendedMatches(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting recommended matches for user: {} (limit: {})", userId, limit);
        List<MatchResponse> matches = matchingService.getRecommendedMatches(userId, limit);
        return ResponseEntity.ok(matches);
    }
}

