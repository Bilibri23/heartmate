package org.rooms.roombay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.response.FeedSectionResponse;
import org.rooms.roombay.dto.response.HomeFeedResponse;
import org.rooms.roombay.dto.response.ListingResponse;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private RecommendationService recommendationService;
    @Mock
    private ListingService listingService;
    @Mock
    private PropertyListingRepository listingRepository;

    private FeedService feedService;

    @BeforeEach
    void setUp() {
        feedService = new FeedService(
                recommendationService,
                Optional.empty(),
                listingService,
                listingRepository
        );
    }

    @Test
    void buildFeedIncludesVideoTourCountAndReelsSection() {
        UUID listingId = UUID.randomUUID();
        PropertyListing withVideo = PropertyListing.builder()
                .id(listingId)
                .title("Studio with tour")
                .videoTourUrl("https://res.cloudinary.com/demo/video.mp4")
                .status(PropertyListing.Status.ACTIVE)
                .build();
        ListingResponse response = ListingResponse.builder().id(listingId).title("Studio with tour").build();

        when(listingRepository.countActiveWithVideoTour()).thenReturn(5L);
        when(listingRepository.findActiveWithPlayableVideoTour(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(withVideo)));
        when(listingService.toListingResponse(eq(withVideo), any())).thenReturn(response);

        HomeFeedResponse feed = feedService.buildFeed("reels", 12, "en", null);

        assertThat(feed.getVideoTourListingCount()).isEqualTo(5L);
        assertThat(feed.getReels()).isNotNull();
        assertThat(feed.getReels().getItems()).hasSize(1);
        assertThat(feed.getReels().getItems().get(0).getTitle()).isEqualTo("Studio with tour");
        verify(listingRepository).findActiveWithPlayableVideoTour(any(Pageable.class));
    }

    @Test
    void buildFeedReelsOrdersByViewsAndRecencyViaRepository() {
        when(listingRepository.countActiveWithVideoTour()).thenReturn(3L);
        when(listingRepository.findActiveWithPlayableVideoTour(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        FeedSectionResponse reels = feedService.buildFeed("reels", 8, "en", UUID.randomUUID()).getReels();

        assertThat(reels.getTotal()).isZero();
        assertThat(reels.getItems()).isEmpty();
    }
}
