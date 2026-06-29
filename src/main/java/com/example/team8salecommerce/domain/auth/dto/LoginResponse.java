package com.example.team8salecommerce.domain.auth.dto;

/**
 * 로그인 성공 시 클라이언트에게 내려주는 응답입니다.
 *
 * <p>클라이언트는 {@code accessToken}을 저장해 두었다가 보호된 API를 호출할 때
 * {@code Authorization: Bearer accessToken값} 형태로 보냅니다.</p>
 *
 * @param memberId 로그인한 회원 ID
 * @param email 로그인한 회원 이메일
 * @param nickname 로그인한 회원 닉네임
 * @param role 로그인한 회원 역할
 * @param accessToken API 인증에 사용할 토큰
 * @param refreshToken Access Token 재발급에 사용할 토큰
 * @param tokenType Authorization 헤더에 붙일 토큰 타입
 */
public record LoginResponse(
        Long memberId,
        String email,
        String nickname,
        String role,
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
