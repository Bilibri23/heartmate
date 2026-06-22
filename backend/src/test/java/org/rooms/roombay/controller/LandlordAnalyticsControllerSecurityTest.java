package org.rooms.roombay.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class LandlordAnalyticsControllerSecurityTest {

    @Test
    void analyticsControllerIsMountedUnderLandlordApi() {
        RequestMapping mapping = LandlordAnalyticsController.class.getAnnotation(RequestMapping.class);

        org.assertj.core.api.Assertions.assertThat(mapping).isNotNull();
        org.assertj.core.api.Assertions.assertThat(mapping.value()).containsExactly("/api/landlord/analytics");
    }

    @Test
    void analyticsControllerRequiresLandlordRole() {
        PreAuthorize preAuthorize = LandlordAnalyticsController.class.getAnnotation(PreAuthorize.class);

        org.assertj.core.api.Assertions.assertThat(preAuthorize).isNotNull();
        org.assertj.core.api.Assertions.assertThat(preAuthorize.value()).isEqualTo("hasRole('LANDLORD')");
    }

    @Test
    void analyticsEndpointUsesGetMapping() throws NoSuchMethodException {
        GetMapping getMapping = LandlordAnalyticsController.class
                .getMethod("getAnalytics", String.class)
                .getAnnotation(GetMapping.class);

        org.assertj.core.api.Assertions.assertThat(getMapping).isNotNull();
    }
}
