package org.rooms.roombuddy.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.rooms.roombuddy.exception.BadRequestException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationPolicyServiceTest {
    private final AuthorizationPolicyService service = new AuthorizationPolicyService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsCurrentUserAccess() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
        );

        assertDoesNotThrow(() -> service.assertCurrentUserOrAdmin(userId));
    }

    @Test
    void blocksCrossUserForNonAdmin() {
        UUID current = UUID.randomUUID();
        UUID requested = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(current, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
        );

        assertThrows(BadRequestException.class, () -> service.assertCurrentUserOrAdmin(requested));
    }
}
