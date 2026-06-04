package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void normalizeRangeSupportsOpsWindows() {
        assertThat((String) ReflectionTestUtils.invokeMethod(AdminOpsService.class, "normalizeRange", "24h")).isEqualTo("24h");
        assertThat((String) ReflectionTestUtils.invokeMethod(AdminOpsService.class, "normalizeRange", "7d")).isEqualTo("7d");
        assertThat((String) ReflectionTestUtils.invokeMethod(AdminOpsService.class, "normalizeRange", "30d")).isEqualTo("30d");
        assertThat((String) ReflectionTestUtils.invokeMethod(AdminOpsService.class, "normalizeRange", "90d")).isEqualTo("90d");
        assertThat((String) ReflectionTestUtils.invokeMethod(AdminOpsService.class, "normalizeRange", "bad")).isEqualTo("7d");
    }
}
