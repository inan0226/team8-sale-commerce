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

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public JwtTokenProvider(
            @Value("${jwt.secret:team8-sale-commerce-default-jwt-secret-key-for-local-development}") String secret,
            @Value("${jwt.access-token-expiration:1800000}") long accessTokenExpirationMillis,
            @Value("${jwt.refresh-token-expiration:1209600000}") long refreshTokenExpirationMillis
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
    }

    public String createAccessToken(Member member) {
        return createToken(member, accessTokenExpirationMillis);
    }

    public String createRefreshToken(Member member) {
        return createToken(member, refreshTokenExpirationMillis);
    }

    public void validateToken(String token) {
        parseClaims(token);
    }

    public Long getMemberId(String token) {
        return parseClaims(token).get("memberId", Long.class);
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Role getRole(String token) {
        return Role.valueOf(parseClaims(token).get("role", String.class));
    }

    public long getRemainingExpirationMillis(String token) {
        return parseClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }

    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpirationMillis;
    }

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
