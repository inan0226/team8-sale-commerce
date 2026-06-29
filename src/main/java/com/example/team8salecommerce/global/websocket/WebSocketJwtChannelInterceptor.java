package com.example.team8salecommerce.global.websocket;

import com.example.team8salecommerce.domain.chat.service.ChatService;
import com.example.team8salecommerce.domain.member.entity.Role;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.AuthMemberResolver;
import com.example.team8salecommerce.global.security.BearerTokenResolver;
import com.example.team8salecommerce.global.security.JwtAuthenticationFilter;
import com.example.team8salecommerce.global.security.JwtAuthenticationService;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 채팅 WebSocket으로 들어오는 STOMP 프레임의 인증과 인가를 처리하는 인터셉터입니다.
 *
 * <p>초보자용으로 흐름을 풀면 다음과 같습니다.</p>
 * <ol>
 *     <li>클라이언트가 STOMP CONNECT 프레임에 JWT를 담아 보냅니다.</li>
 *     <li>인터셉터가 JWT를 검증하고, 인증된 회원 정보를 Principal로 저장합니다.</li>
 *     <li>이후 SEND, SUBSCRIBE 프레임은 CONNECT 때 저장한 Principal을 재사용합니다.</li>
 *     <li>채팅방 구독은 해당 회원이 그 방에 접근할 수 있는지 한 번 더 확인합니다.</li>
 * </ol>
 *
 * <p>즉, HTTP API의 JWT 인증은 {@link JwtAuthenticationFilter}가 맡고,
 * 채팅 STOMP 메시지의 JWT 인증은 이 인터셉터가 맡습니다.</p>
 */
@Component
@RequiredArgsConstructor
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_ROOM_TOPIC_PATTERN = Pattern.compile("^/sub/chat/rooms/(\\d+)$");

    private final BearerTokenResolver bearerTokenResolver;
    private final JwtAuthenticationService jwtAuthenticationService;
    private final AuthMemberResolver authMemberResolver;
    private final ChatService chatService;

    /**
     * STOMP 프레임이 실제 컨트롤러로 전달되기 전에 실행됩니다.
     *
     * <p>CONNECT는 로그인 확인을 하는 단계이고, SEND/SUBSCRIBE는 이미 인증된 사용자인지 확인하는 단계입니다.
     * SUBSCRIBE는 추가로 채팅방 접근 권한까지 확인합니다.</p>
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
            return message;
        }

        if (requiresAuthentication(accessor.getCommand()) && accessor.getUser() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateChatRoomSubscription(accessor.getUser(), accessor.getDestination());
        }

        return message;
    }

    /**
     * STOMP CONNECT 프레임의 Authorization 헤더에서 JWT를 꺼내 인증 객체를 만듭니다.
     *
     * <p>성공하면 반환된 Principal이 WebSocket 세션에 저장되고,
     * 이후 메시지 처리에서 현재 사용자 정보로 사용됩니다.</p>
     */
    private Principal authenticate(String authorizationHeader) {
        return jwtAuthenticationService.authenticate(bearerTokenResolver.resolve(authorizationHeader));
    }

    /**
     * 인증이 필요한 STOMP 명령인지 판단합니다.
     *
     * <p>SEND는 메시지를 보내는 명령이고, SUBSCRIBE는 채팅방 메시지를 받아보기 위한 명령입니다.
     * 두 명령 모두 로그인한 사용자만 사용할 수 있습니다.</p>
     */
    private boolean requiresAuthentication(StompCommand command) {
        return StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command);
    }

    /**
     * 채팅방 구독 요청의 destination을 확인하고 채팅방 접근 권한을 검사합니다.
     *
     * <p>예를 들어 {@code /sub/chat/rooms/10}을 구독하려면,
     * 현재 사용자가 10번 채팅방의 소유자이거나 관리자여야 합니다.
     * 이 검사는 {@link ChatService#validateRoomAccess(Long, Role, Long)}로 위임합니다.</p>
     */
    private void validateChatRoomSubscription(Principal principal, String destination) {
        if (!StringUtils.hasText(destination)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Matcher matcher = CHAT_ROOM_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            AuthMember authMember = authMemberResolver.require(principal);
            chatService.validateRoomAccess(authMember.memberId(), authMember.role(), Long.valueOf(matcher.group(1)));
            return;
        }

        if (destination.startsWith("/sub/chat/")) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
