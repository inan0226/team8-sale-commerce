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
import java.util.UUID;
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

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

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
        return createAccessToken(AuthMember.from(member));
    }

    public String createAccessToken(AuthMember authMember) {
        return createToken(authMember, ACCESS_TOKEN_TYPE, accessTokenExpirationMillis);
    }

    /**
     * Access Token을 다시 발급받을 때 사용할 Refresh Token을 생성합니다.
     */
    public String createRefreshToken(Member member) {
        return createRefreshToken(AuthMember.from(member));
    }

    public String createRefreshToken(AuthMember authMember) {
        return createToken(authMember, REFRESH_TOKEN_TYPE, refreshTokenExpirationMillis);
    }

    public TokenClaims parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, ACCESS_TOKEN_TYPE);

        return toTokenClaims(claims);
    }

    public TokenClaims parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, REFRESH_TOKEN_TYPE);

        return toTokenClaims(claims);
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
    private String createToken(AuthMember authMember, String tokenType, long expirationMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(authMember.email())
                .claim("memberId", authMember.memberId())
                .claim("role", authMember.role().name())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    private void validateTokenType(Claims claims, String expectedTokenType) {
        if (!expectedTokenType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private TokenClaims toTokenClaims(Claims claims) {
        return new TokenClaims(
                claims.get("memberId", Long.class),
                claims.getSubject(),
                Role.valueOf(claims.get("role", String.class)),
                claims.getExpiration().getTime()
        );
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

    public record TokenClaims(
            Long memberId,
            String email,
            Role role,
            long expiresAtMillis
    ) {
    }
}
