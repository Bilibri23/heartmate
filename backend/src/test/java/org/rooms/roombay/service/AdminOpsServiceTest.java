package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminOpsServiceTest {

    @Test
    void conversionRateReturnsZeroWhenDenominatorIsZero() {
        assertThat(AdminOpsService.conversionRate(5, 0)).isEqualTo(0.0);
    }

    @Test
    void conversionRateReturnsRoundedPercentage() {
        assertThat(AdminOpsService.conversionRate(2, 3)).isEqualTo(66.67);
        assertThat(AdminOpsService.conversionRate(1, 4)).isEqualTo(25.0);
    }
}
