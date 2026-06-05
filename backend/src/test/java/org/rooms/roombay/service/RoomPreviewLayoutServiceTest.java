package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.dto.request.RoomPreviewLayoutRequest;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.RoomPreviewLayout;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.RoomPreviewLayoutRepository;
import org.rooms.roombay.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomPreviewLayoutServiceTest {

    private final RoomPreviewLayoutRepository layoutRepository = mock(RoomPreviewLayoutRepository.class);
    private final PropertyListingRepository listingRepository = mock(PropertyListingRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AnalyticsEventService analyticsEventService = mock(AnalyticsEventService.class);
    private final RoomPreviewLayoutService service = new RoomPreviewLayoutService(
            layoutRepository,
            listingRepository,
            userRepository,
            analyticsEventService
    );

    @Test
    void saveLayoutCreatesSanitizedOwnerLayout() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        User user = User.builder().id(userId).role(User.UserRole.STUDENT).build();
        PropertyListing listing = PropertyListing.builder().id(listingId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(layoutRepository.save(any(RoomPreviewLayout.class))).thenAnswer(invocation -> {
            RoomPreviewLayout layout = invocation.getArgument(0);
            layout.setId(UUID.randomUUID());
            return layout;
        });

        var response = service.saveLayout(listingId, userId, "STUDENT", RoomPreviewLayoutRequest.builder()
                .layoutName(" Student room ")
                .items(List.of(Map.of("type", "bed", "x", -1, "y", 0.5, "width", 0.3, "height", 0.2, "rotation", 999)))
                .build());

        assertThat(response.getLayoutName()).isEqualTo("Student room");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0)).containsEntry("type", "BED").containsEntry("x", 0.0).containsEntry("rotation", 180.0);
        verify(analyticsEventService).emit(eq("room_preview_saved"), eq(userId), eq("STUDENT"), eq(listingId), any());
    }

    @Test
    void deleteLayoutRejectsNonOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID layoutId = UUID.randomUUID();
        RoomPreviewLayout layout = RoomPreviewLayout.builder()
                .id(layoutId)
                .user(User.builder().id(ownerId).build())
                .listing(PropertyListing.builder().id(UUID.randomUUID()).build())
                .build();

        when(layoutRepository.findById(layoutId)).thenReturn(Optional.of(layout));

        assertThatThrownBy(() -> service.deleteLayout(layoutId, otherUserId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only the layout owner");
    }
}
