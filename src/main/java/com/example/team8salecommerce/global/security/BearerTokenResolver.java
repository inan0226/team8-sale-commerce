package com.example.team8salecommerce.global.security;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Authorization 헤더에서 Bearer 토큰만 분리해 주는 도우미 클래스입니다.
 *
 * <p>클라이언트는 보통 {@code Authorization: Bearer eyJ...} 형태로 토큰을 보냅니다.
 * 실제 JWT 검증에는 {@code Bearer } 접두사가 필요 없으므로, 이 클래스가 순수 토큰 문자열만 잘라냅니다.</p>
 */
@Component
public class BearerTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 토큰이 반드시 필요한 요청에서 Bearer 토큰을 꺼냅니다.
     *
     * <p>헤더가 없거나 {@code Bearer } 형식이 아니면 인증되지 않은 요청으로 보고 예외를 던집니다.</p>
     */
    public String resolve(String authorizationHeader) {
        return resolveOptional(authorizationHeader)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 토큰이 없어도 되는 요청에서 Bearer 토큰을 조심스럽게 꺼냅니다.
     *
     * <p>예를 들어 회원가입, 로그인, 공개 상품 조회 API는 토큰 없이 들어올 수 있습니다.
     * 그래서 필터에서는 예외를 바로 던지지 않고 {@link Optional#empty()}로 반환합니다.</p>
     */
    public Optional<String> resolveOptional(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        return Optional.of(authorizationHeader.substring(BEARER_PREFIX.length()));
    }
}
