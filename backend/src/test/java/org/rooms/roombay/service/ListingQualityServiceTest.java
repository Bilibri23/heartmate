package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.dto.response.PhotoDTO;
import org.rooms.roombay.entity.PropertyListing;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListingQualityServiceTest {

    @Test
    void qualityScoreReflectsCompletedSignals() {
        PropertyListing listing = PropertyListing.builder()
                .title("Bright studio near campus")
                .description("A clean and well described listing with enough practical detail for tenants to understand the space before applying.")
                .city("Douala")
                .neighborhood("Bonamoussadi")
                .rentAmount(150000)
                .amenities(List.of("Wifi", "Water", "Security"))
                .verified(true)
                .availableFrom(LocalDate.now())
                .videoTourUrl("https://res.cloudinary.com/demo/video/upload/tour.mp4")
                .build();
        List<PhotoDTO> photos = List.of(photo(), photo(), photo());

        Map<String, Boolean> signals = ListingService.qualitySignals(listing, photos);

        assertThat(signals).allSatisfy((key, value) -> assertThat(value).as(key).isTrue());
        assertThat(ListingService.qualityScore(signals)).isEqualTo(100);
    }

    @Test
    void qualityScoreHandlesMissingSignals() {
        PropertyListing listing = PropertyListing.builder()
                .title("Room")
                .rentAmount(0)
                .verified(false)
                .build();

        Map<String, Boolean> signals = ListingService.qualitySignals(listing, List.of());

        assertThat(ListingService.qualityScore(signals)).isLessThan(50);
    }

    private static PhotoDTO photo() {
        return PhotoDTO.builder().photoUrl("https://res.cloudinary.com/demo/image/upload/room.jpg").build();
    }
}
