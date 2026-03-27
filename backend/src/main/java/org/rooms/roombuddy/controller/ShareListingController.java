package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.ShareListingRequest;
import org.rooms.roombay.service.ShareListingService;
import org.rooms.roombay.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/share-listing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Share Listing", description = "Share a listing with a matched roommate")
public class ShareListingController {

    private final ShareListingService shareListingService;

    @PostMapping
    @Operation(summary = "Share listing with roommate", 
               description = "Send a notification with listing link to a matched roommate. Roommate can apply independently.")
    public ResponseEntity<Void> shareListing(@Valid @RequestBody ShareListingRequest request) {
        var userId = SecurityUtils.getCurrentUserId();
        log.info("User {} sharing listing {} with roommate {}", userId, request.getListingId(), request.getRoommateId());
        shareListingService.shareListing(userId, request);
        return ResponseEntity.ok().build();
    }
}
