package org.rooms.roombuddy.controller;

import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.rooms.roombuddy.entity.ListingPhoto;
import org.rooms.roombuddy.entity.PropertyListing;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.repository.ListingPhotoRepository;
import org.rooms.roombuddy.repository.PropertyListingRepository;
import org.rooms.roombuddy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/dev")
@Profile("!prod")
@RequiredArgsConstructor
public class DevSentryController {
    private final UserRepository userRepository;
    private final PropertyListingRepository propertyListingRepository;
    private final ListingPhotoRepository listingPhotoRepository;
    private final Random random = new Random();

    @Value("${sentry.dsn:}")
    private String sentryDsn;

    @Value("${sentry.environment:}")
    private String sentryEnvironment;

    @GetMapping("/sentry-test")
    public ResponseEntity<Map<String, Object>> sentryTest() {
        Exception error = new Exception("Sentry test error from Spring Boot API");
        String eventId = Sentry.captureException(error).toString();
        Sentry.flush(5000);

        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", false);
        payload.put("message", error.getMessage());
        payload.put("eventId", eventId);
        payload.put("environment", (sentryEnvironment == null || sentryEnvironment.isBlank()) ? "unset" : sentryEnvironment);
        payload.put("sentryDsnPresent", sentryDsn != null && !sentryDsn.isBlank());
        payload.put("flushed", true);
        return ResponseEntity.status(500).body(payload);
    }

    @PostMapping("/verify-all-users")
    public ResponseEntity<Map<String, Object>> verifyAllUsers() {
        List<User> users = userRepository.findAll();
        int updated = 0;
        for (User user : users) {
            boolean changed = false;
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                user.setEmailVerified(true);
                changed = true;
            }
            if (!Boolean.TRUE.equals(user.getPhoneVerified())) {
                user.setPhoneVerified(true);
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
                updated++;
            }
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("totalUsers", users.size());
        payload.put("updatedUsers", updated);
        payload.put("message", "Users marked as email+phone verified");
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/listing-stats")
    public ResponseEntity<Map<String, Object>> listingStats() {
        List<PropertyListing> all = propertyListingRepository.findAll();
        long active = all.stream().filter(l -> l.getStatus() == PropertyListing.Status.ACTIVE).count();
        long activeVerified = all.stream().filter(l -> l.getStatus() == PropertyListing.Status.ACTIVE && Boolean.TRUE.equals(l.getVerified())).count();

        Map<String, Object> payload = new HashMap<>();
        payload.put("totalListings", all.size());
        payload.put("activeListings", active);
        payload.put("activeVerifiedListings", activeVerified);
        payload.put("activeFeedListings", propertyListingRepository.findActiveVerifiedListings().size());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/seed-listings")
    public ResponseEntity<Map<String, Object>> seedListings(
            @RequestParam(defaultValue = "17") int count,
            @RequestParam(defaultValue = "false") boolean forceAppend) {
        List<PropertyListing> existing = propertyListingRepository.findAll();
        if (!forceAppend && !existing.isEmpty()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("seeded", 0);
            payload.put("existingListings", existing.size());
            payload.put("message", "Listings already exist. Use forceAppend=true to add more.");
            return ResponseEntity.ok(payload);
        }

        Optional<User> landlordOptional = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.LANDLORD)
                .findFirst();
        User landlord = landlordOptional.orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
        if (landlord == null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("seeded", 0);
            payload.put("message", "No users available to attach listings.");
            return ResponseEntity.badRequest().body(payload);
        }

        List<String> titles = List.of(
                "Modern Studio Near Campus",
                "Bright Apartment in Bastos",
                "Affordable Shared Room",
                "Private Room with Balcony",
                "Clean House in Buea",
                "Student Apartment in Akwa",
                "Calm Residence in Melen",
                "Furnished Studio in Bonapriso",
                "Secure Home Near University",
                "Spacious Flat in Yaounde",
                "Cozy Room in Limbe",
                "Family Apartment in Douala",
                "Newly Renovated Studio",
                "Peaceful House in Kribi",
                "Budget Room in Bamenda",
                "Premium Apartment Downtown",
                "Comfortable Shared Apartment"
        );
        List<String> cities = List.of("Douala", "Yaounde", "Buea", "Limbe", "Bamenda");
        List<String> neighborhoods = List.of("Bonapriso", "Akwa", "Bastos", "Melen", "Up Station", "Makepe");
        List<PropertyListing.PropertyType> propertyTypes = List.of(
                PropertyListing.PropertyType.STUDIO,
                PropertyListing.PropertyType.APARTMENT,
                PropertyListing.PropertyType.HOUSE,
                PropertyListing.PropertyType.PRIVATE_ROOM,
                PropertyListing.PropertyType.SHARED_ROOM
        );

        int seeded = 0;
        for (int i = 0; i < count; i++) {
            PropertyListing listing = PropertyListing.builder()
                    .landlord(landlord)
                    .title(titles.get(i % titles.size()))
                    .description("Well-maintained property with clean rooms, secure neighborhood, and quick access to transport and campus.")
                    .propertyType(propertyTypes.get(i % propertyTypes.size()))
                    .rentAmount(35000 + (i * 5000))
                    .deposit(20000 + (i * 3000))
                    .agencyFees(0)
                    .region("Littoral")
                    .city(cities.get(i % cities.size()))
                    .neighborhood(neighborhoods.get(i % neighborhoods.size()))
                    .address("Street " + (10 + i) + ", " + neighborhoods.get(i % neighborhoods.size()))
                    .bedrooms(1 + (i % 3))
                    .bathrooms(1 + (i % 2))
                    .squareMeters(20 + (i * 3))
                    .amenities(sampleAmenities(i))
                    .videoTourUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                    .virtualTourProvider("generic")
                    .videoTourThumbnail("https://picsum.photos/seed/room-video-" + i + "/640/360")
                    .videoTourDuration(40 + random.nextInt(120))
                    .availableFrom(LocalDate.now().plusDays(i % 15))
                    .status(PropertyListing.Status.ACTIVE)
                    .verified(true)
                    .featured(i % 5 == 0)
                    .landlordWhatsapp("+23767000000" + (i % 10))
                    .viewsCount(30 + random.nextInt(300))
                    .favoritesCount(2 + random.nextInt(40))
                    .averageRating(4.0 + (random.nextDouble() * 1.0))
                    .reviewCount(1 + random.nextInt(18))
                    .verifiedBy(landlord)
                    .verifiedAt(LocalDateTime.now())
                    .build();
            PropertyListing saved = propertyListingRepository.save(listing);

            List<ListingPhoto> photos = new ArrayList<>();
            photos.add(ListingPhoto.builder()
                    .listing(saved)
                    .photoUrl("https://picsum.photos/seed/room-main-" + i + "/1200/900")
                    .isPrimary(true)
                    .displayOrder(0)
                    .build());
            photos.add(ListingPhoto.builder()
                    .listing(saved)
                    .photoUrl("https://picsum.photos/seed/room-living-" + i + "/1200/900")
                    .isPrimary(false)
                    .displayOrder(1)
                    .build());
            photos.add(ListingPhoto.builder()
                    .listing(saved)
                    .photoUrl("https://picsum.photos/seed/room-kitchen-" + i + "/1200/900")
                    .isPrimary(false)
                    .displayOrder(2)
                    .build());
            listingPhotoRepository.saveAll(photos);
            seeded++;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("seeded", seeded);
        payload.put("message", "Dev listings seeded successfully");
        payload.put("activeFeedListings", propertyListingRepository.findActiveVerifiedListings().size());
        return ResponseEntity.ok(payload);
    }

    private List<String> sampleAmenities(int index) {
        List<String> all = List.of("WiFi", "Furnished", "Parking", "Security", "Kitchen", "Generator", "Water Tank");
        List<String> picked = new ArrayList<>();
        picked.add(all.get(index % all.size()));
        picked.add(all.get((index + 2) % all.size()));
        picked.add(all.get((index + 4) % all.size()));
        return picked;
    }
}
