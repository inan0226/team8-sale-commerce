package com.example.team8salecommerce.global.websocket;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.team8salecommerce.domain.chat.service.ChatService;
import com.example.team8salecommerce.domain.member.entity.Role;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private WebSocketJwtChannelInterceptor interceptor;

    @Test
    void subscribe_validatesChatRoomAccess() {
        Message<?> message = subscribeMessage(1L, "/sub/chat/rooms/10");

        interceptor.preSend(message, mock(MessageChannel.class));

        verify(chatService).validateRoomAccess(1L, 10L);
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
