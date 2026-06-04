package org.rooms.roombay.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancedRateLimitInterceptorTest {

    @Test
    void localFallbackAllowsRequestsWhenRedisIsNotRequired() {
        AdvancedRateLimitInterceptor interceptor = new AdvancedRateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "userLimitPerMinute", 100);
        ReflectionTestUtils.setField(interceptor, "ipLimitPerMinute", 50);
        ReflectionTestUtils.setField(interceptor, "loginLimitPerMinute", 10);
        ReflectionTestUtils.setField(interceptor, "registerLimitPerMinute", 5);
        ReflectionTestUtils.setField(interceptor, "listingCreateLimitPerMinute", 4);
        ReflectionTestUtils.setField(interceptor, "redisRequired", false);

        boolean allowed = interceptor.preHandle(request("/api/listings"), new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
        assertThat(interceptor.getCurrentMode()).isEqualTo(AdvancedRateLimitInterceptor.RateLimitMode.LOCAL);
    }

    @Test
    void redisRequiredFailsClosedWhenNoRedisTemplateExists() {
        AdvancedRateLimitInterceptor interceptor = new AdvancedRateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "userLimitPerMinute", 100);
        ReflectionTestUtils.setField(interceptor, "ipLimitPerMinute", 50);
        ReflectionTestUtils.setField(interceptor, "loginLimitPerMinute", 10);
        ReflectionTestUtils.setField(interceptor, "registerLimitPerMinute", 5);
        ReflectionTestUtils.setField(interceptor, "listingCreateLimitPerMinute", 4);
        ReflectionTestUtils.setField(interceptor, "redisRequired", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request("/api/listings"), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(interceptor.getCurrentMode()).isEqualTo(AdvancedRateLimitInterceptor.RateLimitMode.UNAVAILABLE);
    }

    @Test
    void listingCreationLimitAppliesOnlyToPostListings() {
        AdvancedRateLimitInterceptor interceptor = new AdvancedRateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "userLimitPerMinute", 100);
        ReflectionTestUtils.setField(interceptor, "ipLimitPerMinute", 100);
        ReflectionTestUtils.setField(interceptor, "loginLimitPerMinute", 10);
        ReflectionTestUtils.setField(interceptor, "registerLimitPerMinute", 5);
        ReflectionTestUtils.setField(interceptor, "listingCreateLimitPerMinute", 4);
        ReflectionTestUtils.setField(interceptor, "redisRequired", false);

        assertThat(interceptor.preHandle(request("GET", "/api/listings"), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(request("POST", "/api/listings"), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(request("POST", "/api/listings"), new MockHttpServletResponse(), new Object())).isTrue();
        MockHttpServletResponse blocked = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request("POST", "/api/listings"), blocked, new Object());

        assertThat(allowed).isFalse();
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    private static MockHttpServletRequest request(String path) {
        return request("GET", path);
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
