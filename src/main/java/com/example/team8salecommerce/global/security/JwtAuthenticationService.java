package com.example.team8salecommerce.global.security;

import com.example.team8salecommerce.domain.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * JWT 토큰을 Spring Security가 이해하는 인증 객체로 바꾸는 서비스입니다.
 *
 * <p>JWT에는 회원 ID가 들어 있고, Spring Security는 {@link Authentication} 객체를 사용합니다.
 * 이 클래스는 토큰에서 회원 ID를 꺼낸 뒤 Redis 인증 스냅샷을 조회해
 * {@link AuthMember}를 principal로 가진 인증 객체를 만들어 줍니다.</p>
 *
 * <p>HTTP 요청과 채팅 WebSocket 연결이 같은 인증 객체를 사용하도록 이 로직을 공통화했습니다.</p>
 */
@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * JWT를 검증한 뒤, 해당 회원의 인증 객체를 생성합니다.
     *
     * <p>토큰 검증에 실패하거나 Redis 인증 세션이 없으면 예외가 발생합니다.
     * 정상적으로 끝나면 Spring Security가 사용할 수 있는 {@link Authentication}이 반환됩니다.</p>
     */
    public Authentication authenticate(String token) {
        JwtTokenProvider.TokenClaims claims = jwtTokenProvider.parseAccessToken(token);
        AuthMember authMember = refreshTokenService.authenticateAccessToken(token, claims.memberId());

        return createAuthentication(authMember);
    }

    /**
     * AuthMember를 Spring Security 인증 객체로 감쌉니다.
     *
     * <p>첫 번째 인자인 principal에는 "현재 로그인한 사용자" 정보를 넣고,
     * 세 번째 인자인 authorities에는 USER, ADMIN 같은 권한 정보를 넣습니다.</p>
     */
    private Authentication createAuthentication(AuthMember authMember) {
        return new UsernamePasswordAuthenticationToken(
                authMember,
                null,
                authMember.getAuthorities()
        );
    }
}
