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

import java.security.Principal;
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
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            rejectSubscription("missing destination", null);
        }
        if (user == null || !StringUtils.hasText(user.getName())) {
            rejectSubscription("missing principal", destination);
        }
        if (destination.startsWith("/user/queue/")) {
            return;
        }
        if (destination.startsWith("/user/")) {
            String[] parts = destination.split("/");
            if (parts.length < 4 || !user.getName().equals(parts[2]) || !"queue".equals(parts[3])) {
                rejectSubscription("cross-user private destination", destination);
            }
            return;
        }
        if (destination.startsWith("/queue/")) {
            rejectSubscription("raw queue destination", destination);
        }
        if (destination.startsWith("/topic/")) {
            if (destination.equals("/topic/listings")
                    || destination.equals("/topic/listings/new")
                    || destination.equals("/topic/online-status")) {
                return;
            }
            rejectSubscription("unsupported topic", destination);
        }
    }

    private void rejectSubscription(String reason, String destination) {
        String message = "WebSocket SUBSCRIBE rejected: " + reason + (destination != null ? " destination=" + destination : "");
        log.warn(message);
        appErrorLogService.log("WARN", "WEBSOCKET", message, "/ws", null);
        throw new AccessDeniedException("Unauthorized WebSocket subscription");
    }
}
