package com.example.team8salecommerce.domain.auth.service;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;
    private final Map<Long, String> fallbackRefreshTokens = new ConcurrentHashMap<>();
    private final Map<String, Long> fallbackBlacklistedAccessTokens = new ConcurrentHashMap<>();

    @Value("${auth.redis-required:true}")
    private boolean redisRequired;

    public void saveRefreshToken(Long memberId, String refreshToken, long expirationMillis) {
        try {
            stringRedisTemplate.opsForValue()
                    .set(REFRESH_TOKEN_PREFIX + memberId, refreshToken, Duration.ofMillis(expirationMillis));
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("refresh token 저장", exception);
            fallbackRefreshTokens.put(memberId, refreshToken);
        }
    }

    public void deleteRefreshToken(Long memberId) {
        try {
            stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + memberId);
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("refresh token 삭제", exception);
        }

        fallbackRefreshTokens.remove(memberId);
    }

    public void blacklistAccessToken(String accessToken, long expirationMillis) {
        if (expirationMillis <= 0) {
            return;
        }

        try {
            stringRedisTemplate.opsForValue()
                    .set(BLACKLIST_PREFIX + accessToken, "logout", Duration.ofMillis(expirationMillis));
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("access token blacklist 등록", exception);
            fallbackBlacklistedAccessTokens.put(accessToken, System.currentTimeMillis() + expirationMillis);
        }
    }

    public boolean isBlacklisted(String accessToken) {
        removeExpiredFallbackBlacklist(accessToken);

        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + accessToken))
                    || fallbackBlacklistedAccessTokens.containsKey(accessToken);
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("access token blacklist 조회", exception);
            return fallbackBlacklistedAccessTokens.containsKey(accessToken);
        }
    }

    private void handleRedisFailure(String operation, RedisConnectionFailureException exception) {
        if (redisRequired) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_STORE_UNAVAILABLE);
        }

        log.warn("Redis 연결 실패로 {} 작업을 메모리 fallback으로 처리합니다.", operation, exception);
    }

    private void removeExpiredFallbackBlacklist(String accessToken) {
        Long expirationTime = fallbackBlacklistedAccessTokens.get(accessToken);
        if (expirationTime != null && expirationTime <= System.currentTimeMillis()) {
            fallbackBlacklistedAccessTokens.remove(accessToken);
        }
    }
}
