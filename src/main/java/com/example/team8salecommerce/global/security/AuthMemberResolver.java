package com.example.team8salecommerce.global.security;

import com.example.team8salecommerce.domain.member.entity.Role;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 컨트롤러와 WebSocket 인터셉터에서 인증 회원을 안전하게 꺼내는 도우미입니다.
 *
 * <p>HTTP 요청에서는 {@code @AuthenticationPrincipal AuthMember}로 회원을 받고,
 * WebSocket/STOMP에서는 {@link Principal} 안에 들어 있는 인증 객체에서 회원을 꺼냅니다.
 * 두 방식의 null 처리와 예외 처리를 이 클래스에 모아 중복 코드를 줄입니다.</p>
 */
@Component
public class AuthMemberResolver {

    /**
     * HTTP 컨트롤러에서 받은 AuthMember가 실제로 존재하는지 확인합니다.
     *
     * <p>null이면 로그인하지 않은 요청이므로 401 Unauthorized 예외를 던집니다.</p>
     */
    public AuthMember require(AuthMember authMember) {
        if (authMember == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return authMember;
    }

    /**
     * STOMP 메시지에서 넘어온 Principal 안의 AuthMember를 꺼냅니다.
     *
     * <p>WebSocket 연결 시점에 {@link com.example.team8salecommerce.global.websocket.WebSocketJwtChannelInterceptor}
     * 가 Principal을 만들어 두기 때문에, 메시지 처리 시점에는 여기서 꺼내 쓰면 됩니다.</p>
     */
    public AuthMember require(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthMember authMember) {
            return authMember;
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 로그인 여부와 역할 권한을 함께 확인합니다.
     *
     * <p>예를 들어 채팅방 상태 변경은 관리자만 가능하므로,
     * {@code requireRole(authMember, Role.ADMIN)}처럼 호출합니다.</p>
     */
    public AuthMember requireRole(AuthMember authMember, Role requiredRole) {
        AuthMember authenticatedMember = require(authMember);
        if (authenticatedMember.role() != requiredRole) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return authenticatedMember;
    }
}
