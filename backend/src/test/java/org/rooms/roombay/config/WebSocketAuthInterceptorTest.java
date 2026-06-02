package org.rooms.roombay.config;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.security.JwtTokenProvider;
import org.rooms.roombay.service.AppErrorLogService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final AppErrorLogService appErrorLogService = mock(AppErrorLogService.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(jwtTokenProvider, appErrorLogService);

    @Test
    void connectWithAccessTokenSetsPrincipal() {
        UUID userId = UUID.randomUUID();
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("token")).thenReturn("access");
        when(jwtTokenProvider.getUserIdFromToken("token")).thenReturn(userId);

        Message<?> result = interceptor.preSend(connectMessage("Bearer token"), channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo(userId.toString());
    }

    @Test
    void connectWithoutAuthorizationIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(connectMessage(null), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing WebSocket Authorization");

        verify(appErrorLogService).log(eq("WARN"), eq("WEBSOCKET"), eq("WebSocket CONNECT missing Authorization header"), eq("/ws"), any());
    }

    @Test
    void connectWithRefreshTokenIsRejected() {
        when(jwtTokenProvider.validateToken("refresh")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("refresh")).thenReturn("refresh");

        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer refresh"), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Invalid WebSocket token");
    }

    private static Message<?> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
