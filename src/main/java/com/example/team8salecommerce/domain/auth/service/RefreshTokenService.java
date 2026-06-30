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

/**
 * Refresh Token 저장과 Access Token blacklist 처리를 담당하는 서비스입니다.
 *
 * <p>JWT는 서버 세션을 쓰지 않기 때문에, 한 번 발급된 Access Token은 만료 전까지 기본적으로 유효합니다.
 * 로그아웃한 토큰을 다시 못 쓰게 하려면 "이 토큰은 로그아웃됨"이라는 기록을 별도로 저장해야 합니다.
 * 이 프로젝트에서는 그 기록을 Redis에 저장합니다.</p>
 *
 * <p>테스트나 로컬 환경에서 Redis가 필수가 아닌 설정일 때는 메모리 대체 저장소를 사용합니다.
 * 운영 환경에서는 Redis를 사용하는 것이 일반적입니다.</p>
 */
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

    /**
     * 로그인 성공 시 발급한 Refresh Token을 회원 ID 기준으로 저장합니다.
     *
     * <p>Refresh Token은 Access Token이 만료되었을 때 새 Access Token을 발급받는 데 사용됩니다.
     * 만료 시간이 지나면 Redis에서도 자동으로 사라지도록 TTL을 함께 설정합니다.</p>
     */
    public void saveRefreshToken(Long memberId, String refreshToken, long expirationMillis) {
        try {
            stringRedisTemplate.opsForValue()
                    .set(REFRESH_TOKEN_PREFIX + memberId, refreshToken, Duration.ofMillis(expirationMillis));
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("리프레시 토큰 저장", exception);
            fallbackRefreshTokens.put(memberId, refreshToken);
        }
    }

    /**
     * 로그아웃 시 회원의 Refresh Token을 삭제합니다.
     *
     * <p>Refresh Token을 삭제하면, 이후 해당 Refresh Token으로 새 Access Token을 발급받을 수 없습니다.</p>
     */
    public void deleteRefreshToken(Long memberId) {
        try {
            stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + memberId);
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("리프레시 토큰 삭제", exception);
        }

        fallbackRefreshTokens.remove(memberId);
    }

    /**
     * 로그아웃한 Access Token을 blacklist에 등록합니다.
     *
     * <p>Access Token은 이미 클라이언트가 들고 있는 문자열이라 서버가 직접 회수할 수 없습니다.
     * 대신 남은 만료 시간 동안 blacklist에 저장해, 같은 토큰으로 다시 요청하면 거부하도록 만듭니다.</p>
     */
    public void blacklistAccessToken(String accessToken, long expirationMillis) {
        if (expirationMillis <= 0) {
            return;
        }

        try {
            stringRedisTemplate.opsForValue()
                    .set(BLACKLIST_PREFIX + accessToken, "logout", Duration.ofMillis(expirationMillis));
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("액세스 토큰 블랙리스트 등록", exception);
            fallbackBlacklistedAccessTokens.put(accessToken, System.currentTimeMillis() + expirationMillis);
        }
    }

    /**
     * Access Token이 로그아웃 처리된 토큰인지 확인합니다.
     *
     * <p>{@link com.example.team8salecommerce.global.security.JwtAuthenticationFilter}가 요청마다 이 메서드를 호출해,
     * blacklist에 있는 토큰이면 인증을 거부합니다.</p>
     */
    public boolean isBlacklisted(String accessToken) {
        removeExpiredFallbackBlacklist(accessToken);

        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + accessToken))
                    || fallbackBlacklistedAccessTokens.containsKey(accessToken);
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("액세스 토큰 블랙리스트 조회", exception);
            return fallbackBlacklistedAccessTokens.containsKey(accessToken);
        }
    }

    /**
     * Redis 연결 실패 시 운영 설정에 따라 예외를 던지거나 메모리 대체 저장소를 사용합니다.
     */
    private void handleRedisFailure(String operation, RedisConnectionFailureException exception) {
        if (redisRequired) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_STORE_UNAVAILABLE);
        }

        log.warn("Redis 연결 실패로 {} 작업을 메모리 대체 저장소로 처리합니다.", operation, exception);
    }

    /**
     * 메모리 대체 blacklist에서 만료된 Access Token 기록을 정리합니다.
     */
    private void removeExpiredFallbackBlacklist(String accessToken) {
        Long expirationTime = fallbackBlacklistedAccessTokens.get(accessToken);
        if (expirationTime != null && expirationTime <= System.currentTimeMillis()) {
            fallbackBlacklistedAccessTokens.remove(accessToken);
        }
    }
}
