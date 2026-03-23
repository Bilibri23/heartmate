package org.rooms.roombuddy.security;

import org.rooms.roombuddy.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthorizationPolicyService {
    public void assertCurrentUserOrAdmin(UUID requestedUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();
        if (!currentUserId.equals(requestedUserId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new BadRequestException("Unauthorized access to another user's resources");
        }
    }
}
