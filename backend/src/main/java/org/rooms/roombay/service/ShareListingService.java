package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.ShareListingRequest;
import org.rooms.roombay.entity.Match;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.MatchRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for sharing listings with matched roommates.
 * Simplified from co-apply: sends notification with listing link; roommate can apply independently.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShareListingService {

    private final MatchRepository matchRepository;
    private final PropertyListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public void shareListing(UUID sharerId, ShareListingRequest request) {
        log.info("User {} sharing listing {} with roommate {}", sharerId, request.getListingId(), request.getRoommateId());

        User sharer = userRepository.findById(sharerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User roommate = userRepository.findById(request.getRoommateId())
                .orElseThrow(() -> new ResourceNotFoundException("Roommate not found"));

        PropertyListing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (listing.getStatus() != PropertyListing.Status.ACTIVE) {
            throw new BadRequestException("Can only share active listings");
        }

        Match match = matchRepository.findMatchBetweenUsers(sharerId, request.getRoommateId())
                .orElseThrow(() -> new BadRequestException("You can only share with your matched roommates"));

        if (match.getStatus() != Match.Status.MUTUAL) {
            throw new BadRequestException("You can only share with mutually matched roommates");
        }

        String sharerName = sharer.getFirstName() + " " + sharer.getLastName();
        String actionUrl = frontendUrl + "/listings/" + listing.getId();

        notificationService.createNotification(
                request.getRoommateId(),
                org.rooms.roombay.entity.Notification.NotificationType.LISTING_SHARED,
                "Listing shared with you",
                sharerName + " shared a listing with you: " + listing.getTitle(),
                listing.getId(),
                "LISTING",
                actionUrl
        );

        log.info("Listing {} shared with roommate {} by user {}", listing.getId(), request.getRoommateId(), sharerId);
    }
}
