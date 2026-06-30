package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rooms.roombay.dto.response.AreaStatsResponse;
import org.rooms.roombay.repository.PropertyListingRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceAreaStatsTest {

    @Mock
    private PropertyListingRepository listingRepository;

    @InjectMocks
    private ListingService service;

    @Test
    void mapsAggregationRowsToAreaStats() {
        when(listingRepository.aggregateRentByNeighborhood(null)).thenReturn(List.<Object[]>of(
                new Object[]{"Douala", "Bonapriso", 5L, 125000.0, 80000, 200000}
        ));

        List<AreaStatsResponse> out = service.areaRentStats(null);

        assertThat(out).hasSize(1);
        AreaStatsResponse a = out.get(0);
        assertThat(a.getCity()).isEqualTo("Douala");
        assertThat(a.getNeighborhood()).isEqualTo("Bonapriso");
        assertThat(a.getListingCount()).isEqualTo(5L);
        assertThat(a.getAvgRent()).isEqualTo(125000.0);
        assertThat(a.getMinRent()).isEqualTo(80000);
        assertThat(a.getMaxRent()).isEqualTo(200000);
    }

    @Test
    void blankCityBecomesNullFilter() {
        when(listingRepository.aggregateRentByNeighborhood(null)).thenReturn(List.of());

        assertThat(service.areaRentStats("   ")).isEmpty();
        verify(listingRepository).aggregateRentByNeighborhood(null);
    }
}
