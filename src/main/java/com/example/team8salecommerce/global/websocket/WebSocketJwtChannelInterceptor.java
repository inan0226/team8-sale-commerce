package com.example.team8salecommerce.global.websocket;

import com.example.team8salecommerce.domain.chat.service.ChatService;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.JwtTokenProvider;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_ROOM_TOPIC_PATTERN = Pattern.compile("^/sub/chat/rooms/(\\d+)$");

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final ChatService chatService;

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

    private Principal authenticate(String authorizationHeader) {
        String token = resolveBearerToken(authorizationHeader);
        jwtTokenProvider.validateToken(token);

        Long memberId = jwtTokenProvider.getMemberId(token);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        AuthMember authMember = AuthMember.from(member);

        return new UsernamePasswordAuthenticationToken(
                authMember,
                null,
                authMember.getAuthorities()
        );
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return authorizationHeader.substring(7);
    }

    private boolean requiresAuthentication(StompCommand command) {
        return StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command);
    }

    private void validateChatRoomSubscription(Principal principal, String destination) {
        if (!StringUtils.hasText(destination)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Matcher matcher = CHAT_ROOM_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            AuthMember authMember = resolveAuthMember(principal);
            chatService.validateRoomAccess(authMember.memberId(), Long.valueOf(matcher.group(1)));
        }
    }

    private AuthMember resolveAuthMember(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof AuthMember authMember) {
            return authMember;
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
