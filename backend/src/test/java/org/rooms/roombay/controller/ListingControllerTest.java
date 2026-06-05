package org.rooms.roombay.controller;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListingControllerTest {

    @Test
    void resolveListingIdAcceptsPlainUuidAndSeoSlug() {
        UUID listingId = UUID.randomUUID();

        assertThat(ListingController.resolveListingId(listingId.toString())).isEqualTo(listingId);
        assertThat(ListingController.resolveListingId("modern-studio-douala-" + listingId)).isEqualTo(listingId);
    }
}
