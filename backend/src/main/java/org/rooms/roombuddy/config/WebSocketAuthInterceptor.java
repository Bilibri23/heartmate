package org.rooms.roombay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.rooms.roombay.security.JwtTokenProvider;

import java.util.UUID;

/**
 * Intercepts WebSocket CONNECT frames to authenticate with JWT.
 * Sets the principal for user-specific message routing (convertAndSendToUser).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                        accessor.setUser(() -> userId.toString());
                        log.debug("WebSocket authenticated for user: {}", userId);
                    } else {
                        log.warn("WebSocket CONNECT: invalid token");
                    }
                } catch (Exception e) {
                    log.warn("WebSocket CONNECT: token validation failed: {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket CONNECT: no Authorization header");
            }
        }

        return message;
    }
}
