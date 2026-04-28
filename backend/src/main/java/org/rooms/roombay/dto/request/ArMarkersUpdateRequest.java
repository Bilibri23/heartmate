package org.rooms.roombay.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArMarkersUpdateRequest {
    @NotNull
    @Valid
    @Builder.Default
    private List<ArMarkerItem> markers = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArMarkerItem {
        @NotNull
        private Double x;
        @NotNull
        private Double y;
        @NotNull
        private Double z;
        private String label;
        private String color;
    }
}
