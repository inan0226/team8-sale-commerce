package com.example.team8salecommerce.global.security;

import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * JWT 토큰을 Spring Security가 이해하는 인증 객체로 바꾸는 서비스입니다.
 *
 * <p>JWT에는 회원 ID가 들어 있고, Spring Security는 {@link Authentication} 객체를 사용합니다.
 * 이 클래스는 토큰에서 회원 ID를 꺼내 DB에서 회원을 조회한 뒤,
 * {@link AuthMember}를 principal로 가진 인증 객체를 만들어 줍니다.</p>
 *
 * <p>HTTP 요청과 채팅 WebSocket 연결이 같은 인증 객체를 사용하도록 이 로직을 공통화했습니다.</p>
 */
@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    /**
     * JWT를 검증한 뒤, 해당 회원의 인증 객체를 생성합니다.
     *
     * <p>토큰 검증에 실패하면 예외가 발생하고, 회원이 존재하지 않아도 예외가 발생합니다.
     * 정상적으로 끝나면 Spring Security가 사용할 수 있는 {@link Authentication}이 반환됩니다.</p>
     */
    public Authentication authenticate(String token) {
        jwtTokenProvider.validateToken(token);

        Long memberId = jwtTokenProvider.getMemberId(token);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return createAuthentication(AuthMember.from(member));
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
