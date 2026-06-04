package org.rooms.roombay.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthEndpointRateLimitFilterTest {

    @Test
    void resetPasswordIsCoveredByAuthRateLimiter() {
        AuthEndpointRateLimitFilter filter = new AuthEndpointRateLimitFilter();

        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/auth/reset-password"))).isFalse();
    }
}
