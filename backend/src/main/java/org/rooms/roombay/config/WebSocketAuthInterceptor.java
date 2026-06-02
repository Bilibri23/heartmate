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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.rooms.roombay.security.JwtTokenProvider;
import org.rooms.roombay.service.AppErrorLogService;

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
    private final AppErrorLogService appErrorLogService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (jwtTokenProvider.validateToken(token) && "access".equals(jwtTokenProvider.getTokenType(token))) {
                        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                        accessor.setUser(() -> userId.toString());
                        log.debug("WebSocket authenticated for user: {}", userId);
                        return message;
                    }
                } catch (Exception e) {
                    log.warn("WebSocket CONNECT: token validation failed: {}", e.getMessage());
                    appErrorLogService.log("WARN", "WEBSOCKET", "WebSocket CONNECT token validation failed: " + e.getMessage(), "/ws", e);
                    throw new AccessDeniedException("Invalid WebSocket token", e);
                }

                log.warn("WebSocket CONNECT: invalid token");
                appErrorLogService.log("WARN", "WEBSOCKET", "WebSocket CONNECT invalid token", "/ws", null);
                throw new AccessDeniedException("Invalid WebSocket token");
            } else {
                log.warn("WebSocket CONNECT: no Authorization header");
                appErrorLogService.log("WARN", "WEBSOCKET", "WebSocket CONNECT missing Authorization header", "/ws", null);
                throw new AccessDeniedException("Missing WebSocket Authorization header");
            }
        }

        return message;
    }
}
