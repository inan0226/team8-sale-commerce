package com.example.team8salecommerce.global.security;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RefreshTokenCookieProvider {

    private final String cookieName;
    private final String cookiePath;
    private final boolean secure;
    private final String sameSite;

    public RefreshTokenCookieProvider(
            @Value("${auth.refresh-cookie.name:refreshToken}") String cookieName,
            @Value("${auth.refresh-cookie.path:/auth}") String cookiePath,
            @Value("${auth.refresh-cookie.secure:true}") boolean secure,
            @Value("${auth.refresh-cookie.same-site:Strict}") String sameSite
    ) {
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie create(String refreshToken, long maxAgeMillis) {
        return baseCookie(refreshToken)
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .build();
    }

    public ResponseCookie expire() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String resolve(HttpServletRequest request) {
        return resolveOptional(request)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
    }

    public Optional<String> resolveOptional(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return Optional.of(cookie.getValue());
            }
        }

        return Optional.empty();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(cookiePath);
    }
}
