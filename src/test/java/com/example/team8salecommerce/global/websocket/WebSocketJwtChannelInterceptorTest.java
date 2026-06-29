package com.example.team8salecommerce.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.team8salecommerce.domain.chat.service.ChatService;
import com.example.team8salecommerce.domain.member.entity.Role;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.AuthMemberResolver;
import com.example.team8salecommerce.global.security.BearerTokenResolver;
import com.example.team8salecommerce.global.security.JwtAuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class WebSocketJwtChannelInterceptorTest {

    @Mock
    private BearerTokenResolver bearerTokenResolver;

    @Mock
    private JwtAuthenticationService jwtAuthenticationService;

    @Spy
    private AuthMemberResolver authMemberResolver;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private WebSocketJwtChannelInterceptor interceptor;

    @Test
    void connect_authenticatesWithBearerToken() {
        AuthMember authMember = new AuthMember(1L, "member@example.com", Role.USER);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authMember, null, authMember.getAuthorities());
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer access-token");
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(bearerTokenResolver.resolve("Bearer access-token")).thenReturn("access-token");
        when(jwtAuthenticationService.authenticate("access-token")).thenReturn(authentication);

        interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(accessor.getUser()).isEqualTo(authentication);
    }

    @Test
    void subscribe_validatesChatRoomAccess() {
        Message<?> message = subscribeMessage(1L, "/sub/chat/rooms/10");

        interceptor.preSend(message, mock(MessageChannel.class));

        verify(chatService).validateRoomAccess(1L, Role.USER, 10L);
    }

    @Test
    void subscribe_withoutAuthenticationFails() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/chat/rooms/10");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void subscribe_invalidChatDestinationFails() {
        Message<?> message = subscribeMessage(1L, "/sub/chat/room/10");

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
    }

    private Message<?> subscribeMessage(Long memberId, String destination) {
        AuthMember authMember = new AuthMember(memberId, "member@example.com", Role.USER);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authMember, null, authMember.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(authentication);

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
