package com.example.team8salecommerce.domain.auth.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveRefreshToken(Long memberId, String refreshToken, long expirationMillis) {
        runRedisCommand(() -> stringRedisTemplate.opsForValue()
                .set(REFRESH_TOKEN_PREFIX + memberId, refreshToken, Duration.ofMillis(expirationMillis)));
    }

    public void deleteRefreshToken(Long memberId) {
        runRedisCommand(() -> stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + memberId));
    }

    public void blacklistAccessToken(String accessToken, long expirationMillis) {
        if (expirationMillis <= 0) {
            return;
        }

        runRedisCommand(() -> stringRedisTemplate.opsForValue()
                .set(BLACKLIST_PREFIX + accessToken, "logout", Duration.ofMillis(expirationMillis)));
    }

    public boolean isBlacklisted(String accessToken) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + accessToken));
        } catch (RedisConnectionFailureException exception) {
            return false;
        }
    }

    private void runRedisCommand(Runnable command) {
        try {
            command.run();
        } catch (RedisConnectionFailureException ignored) {
        }
    }
}
