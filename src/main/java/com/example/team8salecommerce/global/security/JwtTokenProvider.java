package com.example.team8salecommerce.global.security;

import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.entity.Role;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT를 만들고 검증하는 클래스입니다.
 *
 * <p>JWT는 로그인 성공 후 클라이언트에게 내려주는 토큰 문자열입니다.
 * 클라이언트는 이후 API를 호출할 때 이 토큰을 Authorization 헤더에 담아 보내고,
 * 서버는 이 클래스에서 토큰이 위조되지 않았는지, 만료되지 않았는지 확인합니다.</p>
 *
 * <p>토큰 안에는 회원 ID, 이메일, 역할 같은 최소한의 인증 정보만 넣습니다.
 * 비밀번호 같은 민감한 정보는 절대 JWT에 넣으면 안 됩니다.</p>
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    /**
     * application.yml 설정값을 사용해 JWT 서명 키와 만료 시간을 준비합니다.
     *
     * <p>서명 키는 토큰 위조 여부를 확인하는 데 쓰입니다.
     * 같은 secret으로 서명한 토큰만 서버가 유효한 토큰으로 받아들입니다.</p>
     */
    public JwtTokenProvider(
            @Value("${jwt.secret:team8-sale-commerce-default-jwt-secret-key-for-local-development}") String secret,
            @Value("${jwt.access-token-expiration:1800000}") long accessTokenExpirationMillis,
            @Value("${jwt.refresh-token-expiration:1209600000}") long refreshTokenExpirationMillis
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
    }

    /**
     * API 요청 인증에 사용할 Access Token을 생성합니다.
     */
    public String createAccessToken(Member member) {
        return createToken(member, accessTokenExpirationMillis);
    }

    /**
     * Access Token을 다시 발급받을 때 사용할 Refresh Token을 생성합니다.
     */
    public String createRefreshToken(Member member) {
        return createToken(member, refreshTokenExpirationMillis);
    }

    /**
     * 토큰이 만료되었거나 위조되었는지 확인합니다.
     *
     * <p>문제가 있으면 {@link CustomException}을 던지고, 문제가 없으면 아무 일도 하지 않습니다.</p>
     */
    public void validateToken(String token) {
        parseClaims(token);
    }

    /**
     * 토큰 안에 저장한 회원 ID를 꺼냅니다.
     */
    public Long getMemberId(String token) {
        return parseClaims(token).get("memberId", Long.class);
    }

    /**
     * 토큰의 subject에 저장한 이메일을 꺼냅니다.
     */
    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰 안에 저장한 회원 역할을 꺼냅니다.
     */
    public Role getRole(String token) {
        return Role.valueOf(parseClaims(token).get("role", String.class));
    }

    /**
     * 토큰이 만료되기까지 남은 시간을 밀리초 단위로 계산합니다.
     *
     * <p>로그아웃 시 Access Token을 blacklist에 넣을 때,
     * 토큰의 남은 시간만큼만 Redis에 저장하기 위해 사용합니다.</p>
     */
    public long getRemainingExpirationMillis(String token) {
        return parseClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }

    /**
     * Refresh Token 만료 시간을 반환합니다.
     */
    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpirationMillis;
    }

    /**
     * Access Token과 Refresh Token이 공통으로 사용하는 실제 JWT 생성 로직입니다.
     */
    private String createToken(Member member, long expirationMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(member.getEmail())
                .claim("memberId", member.getId())
                .claim("role", member.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWT 문자열을 해석해 payload(claims)를 꺼냅니다.
     *
     * <p>서명 검증, 만료 검증도 이 과정에서 함께 일어납니다.
     * 그래서 다른 메서드들은 모두 이 메서드를 거쳐 안전하게 토큰 내용을 읽습니다.</p>
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
}
