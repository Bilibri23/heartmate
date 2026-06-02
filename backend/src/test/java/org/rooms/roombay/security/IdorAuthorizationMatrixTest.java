package org.rooms.roombay.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.rooms.roombay.exception.BadRequestException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdorAuthorizationMatrixTest {
    private final AuthorizationPolicyService service = new AuthorizationPolicyService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "owner can access {0}")
    @ValueSource(strings = {
            "applications",
            "leases",
            "payments",
            "profiles",
            "messages",
            "notifications",
            "listings",
            "reviews",
            "verifications",
            "reports"
    })
    void ownerCanAccessOwnResource(String resourceType) {
        UUID owner = UUID.randomUUID();
        authenticate(owner, "ROLE_STUDENT");

        assertThatCode(() -> service.assertCurrentUserOrAdmin(owner))
                .as(resourceType)
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "non-admin is blocked from another user's {0}")
    @ValueSource(strings = {
            "applications",
            "leases",
            "payments",
            "profiles",
            "messages",
            "notifications",
            "listings",
            "reviews",
            "verifications",
            "reports"
    })
    void nonAdminCannotAccessAnotherUsersResource(String resourceType) {
        authenticate(UUID.randomUUID(), "ROLE_LANDLORD");

        assertThatThrownBy(() -> service.assertCurrentUserOrAdmin(UUID.randomUUID()))
                .as(resourceType)
                .isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest(name = "admin can access {0}")
    @ValueSource(strings = {
            "applications",
            "leases",
            "payments",
            "profiles",
            "messages",
            "notifications",
            "listings",
            "reviews",
            "verifications",
            "reports"
    })
    void adminCanAccessResourceForOpsReview(String resourceType) {
        authenticate(UUID.randomUUID(), "ROLE_ADMIN");

        assertThatCode(() -> service.assertCurrentUserOrAdmin(UUID.randomUUID()))
                .as(resourceType)
                .doesNotThrowAnyException();
    }

    private static void authenticate(UUID userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority(role)))
        );
    }
}
