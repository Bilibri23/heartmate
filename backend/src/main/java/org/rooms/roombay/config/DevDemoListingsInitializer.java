package org.rooms.roombay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Seeds demo listings for thesis demos when explicitly enabled.
 * Set {@code app.dev.seed-listings.enabled=true} on local/dev only.
 */
@Component
@ConditionalOnProperty(name = "app.dev.seed-listings.enabled", havingValue = "true")
@Order(110)
@RequiredArgsConstructor
@Slf4j
public class DevDemoListingsInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PropertyListingRepository listingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (listingRepository.count() > 0) {
            log.info("Dev demo listings seed skipped: listings already exist");
            return;
        }

        User landlord = userRepository.findByEmail("demo-landlord@roombay.local")
                .orElseGet(() -> {
                    String phone = "dm" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
                    User created = User.builder()
                            .email("demo-landlord@roombay.local")
                            .phone(phone)
                            .passwordHash(passwordEncoder.encode("ChangeMe123!"))
                            .firstName("Demo")
                            .lastName("Landlord")
                            .gender(User.Gender.MALE)
                            .role(User.UserRole.LANDLORD)
                            .accountStatus(User.AccountStatus.ACTIVE)
                            .emailVerified(true)
                            .phoneVerified(true)
                            .profileCompleted(true)
                            .build();
                    return userRepository.save(created);
                });

        List<PropertyListing> demos = List.of(
                buildListing(landlord, "Bright studio near Bastos", PropertyListing.PropertyType.STUDIO,
                        PropertyListing.ListingPurpose.RENT, 85000, null,
                        "Yaounde", "Bastos", 1, 1, PropertyListing.FurnishingStatus.FURNISHED, false,
                        PropertyListing.StayType.LONG_TERM, PropertyListing.PricePeriod.MONTH, false),
                buildListing(landlord, "Chambre meublée à Damas", PropertyListing.PropertyType.ROOM,
                        PropertyListing.ListingPurpose.RENT, 45000, null,
                        "Yaounde", "Damas", 1, 1, PropertyListing.FurnishingStatus.FURNISHED, false,
                        PropertyListing.StayType.LONG_TERM, PropertyListing.PricePeriod.MONTH, false),
                buildListing(landlord, "Shared flat — roommate wanted", PropertyListing.PropertyType.SHARED_ROOM,
                        PropertyListing.ListingPurpose.RENT, 35000, null,
                        "Douala", "Bonapriso", 1, 1, PropertyListing.FurnishingStatus.SEMI_FURNISHED, true,
                        PropertyListing.StayType.LONG_TERM, PropertyListing.PricePeriod.MONTH, false),
                buildListing(landlord, "Family house for sale in Bonanjo", PropertyListing.PropertyType.HOUSE,
                        PropertyListing.ListingPurpose.SALE, null, 85000000,
                        "Douala", "Bonanjo", 4, 3, PropertyListing.FurnishingStatus.UNFURNISHED, false,
                        PropertyListing.StayType.LONG_TERM, PropertyListing.PricePeriod.TOTAL, false),
                buildListing(landlord, "Short-stay furnished apartment", PropertyListing.PropertyType.APARTMENT,
                        PropertyListing.ListingPurpose.RENT, 25000, null,
                        "Limbe", "Down Beach", 2, 1, PropertyListing.FurnishingStatus.FURNISHED, false,
                        PropertyListing.StayType.SHORT_TERM, PropertyListing.PricePeriod.NIGHT, false),
                buildListing(landlord, "Unfurnished 2BR with Room Preview", PropertyListing.PropertyType.APARTMENT,
                        PropertyListing.ListingPurpose.RENT, 120000, null,
                        "Yaounde", "Melen", 2, 2, PropertyListing.FurnishingStatus.UNFURNISHED, false,
                        PropertyListing.StayType.LONG_TERM, PropertyListing.PricePeriod.MONTH, true)
        );

        listingRepository.saveAll(demos);
        log.warn("Seeded {} demo listings for landlord {}", demos.size(), landlord.getEmail());
    }

    private static PropertyListing buildListing(
            User landlord,
            String title,
            PropertyListing.PropertyType type,
            PropertyListing.ListingPurpose purpose,
            Integer rent,
            Integer salePrice,
            String city,
            String neighborhood,
            int bedrooms,
            int bathrooms,
            PropertyListing.FurnishingStatus furnishing,
            boolean shared,
            PropertyListing.StayType stayType,
            PropertyListing.PricePeriod pricePeriod,
            boolean roomPreview) {
        return PropertyListing.builder()
                .landlord(landlord)
                .title(title)
                .description("Demo listing seeded for RoomBay showcase.")
                .propertyType(type)
                .listingPurpose(purpose)
                .stayType(stayType)
                .pricePeriod(pricePeriod)
                .rentAmount(rent)
                .salePrice(salePrice)
                .deposit(rent)
                .city(city)
                .neighborhood(neighborhood)
                .bedrooms(bedrooms)
                .bathrooms(bathrooms)
                .furnishingStatus(furnishing)
                .isUnfurnished(furnishing == PropertyListing.FurnishingStatus.UNFURNISHED)
                .isSharedAccommodation(shared)
                .roommatesWanted(shared)
                .availableRoommateSlots(shared ? 1 : null)
                .sharedSpaceNotes(shared ? "Shared kitchen and living room." : null)
                .roomPreviewEnabled(roomPreview)
                .roomPreviewStatus(roomPreview
                        ? PropertyListing.RoomPreviewStatus.APPROVED
                        : PropertyListing.RoomPreviewStatus.NOT_ENABLED)
                .roomLengthMeters(roomPreview ? BigDecimal.valueOf(4.2) : null)
                .roomWidthMeters(roomPreview ? BigDecimal.valueOf(3.5) : null)
                .status(PropertyListing.Status.ACTIVE)
                .verified(true)
                .featured(false)
                .viewsCount(0)
                .favoritesCount(0)
                .build();
    }
}
