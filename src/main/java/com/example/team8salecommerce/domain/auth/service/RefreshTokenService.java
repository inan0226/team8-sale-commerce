package com.example.team8salecommerce.domain.auth.service;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.domain.member.entity.Role;
import com.example.team8salecommerce.global.security.AuthMember;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * 인증 토큰 세션을 Redis에 저장하고 조회하는 서비스입니다.
 *
 * <p>Access Token 인증 시 매번 DB를 조회하지 않도록 로그인 시점의 인증 스냅샷을 Redis에 저장합니다.
 * 필터와 WebSocket 인터셉터는 토큰 검증 후 Redis에서 blacklist 여부와 인증 스냅샷을 한 번에 조회합니다.</p>
 *
 * <p>회원 권한 변경, 정지, 탈퇴처럼 로그인 세션의 권한 정보가 더 이상 유효하지 않은 기능을 추가할 때는
 * {@link #deleteRefreshToken(Long)}을 호출해 refresh token과 인증 스냅샷을 함께 무효화해야 합니다.</p>
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
    private static final String AUTH_MEMBER_PREFIX = "auth:member:";
    private static final RedisScript<String> ROTATE_REFRESH_TOKEN_SCRIPT = RedisScript.of("""
            local storedRefreshToken = redis.call('GET', KEYS[1])
            if not storedRefreshToken or storedRefreshToken ~= ARGV[1] then
                return nil
            end

            local authMember = redis.call('GET', KEYS[2])
            if not authMember then
                return nil
            end

            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            redis.call('PEXPIRE', KEYS[2], ARGV[3])

            return authMember
            """, String.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final Map<Long, String> fallbackRefreshTokens = new ConcurrentHashMap<>();
    private final Map<String, Long> fallbackBlacklistedAccessTokens = new ConcurrentHashMap<>();
    private final Map<Long, AuthMember> fallbackAuthMembers = new ConcurrentHashMap<>();
    private final Object fallbackSessionLock = new Object();

    @Value("${auth.redis-required:true}")
    private boolean redisRequired;

    public void saveLoginSession(AuthMember authMember, String refreshToken, long expirationMillis) {
        Duration expiration = Duration.ofMillis(expirationMillis);

        try {
            stringRedisTemplate.opsForValue()
                    .set(REFRESH_TOKEN_PREFIX + authMember.memberId(), refreshToken, expiration);
            stringRedisTemplate.opsForValue()
                    .set(AUTH_MEMBER_PREFIX + authMember.memberId(), serializeAuthMember(authMember), expiration);
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("로그인 인증 세션 저장", exception);
            fallbackRefreshTokens.put(authMember.memberId(), refreshToken);
            fallbackAuthMembers.put(authMember.memberId(), authMember);
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
            stringRedisTemplate.delete(AUTH_MEMBER_PREFIX + memberId);
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("리프레시 토큰 삭제", exception);
        }

        fallbackRefreshTokens.remove(memberId);
        fallbackAuthMembers.remove(memberId);
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

    public AuthMember authenticateAccessToken(String accessToken, Long memberId) {
        removeExpiredFallbackBlacklist(accessToken);

        try {
            List<String> values = stringRedisTemplate.opsForValue()
                    .multiGet(List.of(BLACKLIST_PREFIX + accessToken, AUTH_MEMBER_PREFIX + memberId));

            if (values != null && values.get(0) != null) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            String authMemberValue = values == null ? null : values.get(1);
            if (authMemberValue == null) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            return deserializeAuthMember(authMemberValue);
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("액세스 토큰 인증 세션 조회", exception);

            if (fallbackBlacklistedAccessTokens.containsKey(accessToken)) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            AuthMember authMember = fallbackAuthMembers.get(memberId);
            if (authMember == null) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            return authMember;
        }
    }

    public AuthMember rotateRefreshToken(
            Long memberId,
            String currentRefreshToken,
            String rotatedRefreshToken,
            long expirationMillis
    ) {
        try {
            String authMemberValue = stringRedisTemplate.execute(
                    ROTATE_REFRESH_TOKEN_SCRIPT,
                    List.of(REFRESH_TOKEN_PREFIX + memberId, AUTH_MEMBER_PREFIX + memberId),
                    currentRefreshToken,
                    rotatedRefreshToken,
                    String.valueOf(expirationMillis)
            );
            if (authMemberValue == null) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            return deserializeAuthMember(authMemberValue);
        } catch (RedisConnectionFailureException exception) {
            handleRedisFailure("리프레시 토큰 원자적 교체", exception);

            synchronized (fallbackSessionLock) {
                String storedRefreshToken = fallbackRefreshTokens.get(memberId);
                if (!currentRefreshToken.equals(storedRefreshToken)) {
                    throw new CustomException(ErrorCode.INVALID_TOKEN);
                }

                AuthMember authMember = fallbackAuthMembers.get(memberId);
                if (authMember == null) {
                    throw new CustomException(ErrorCode.INVALID_TOKEN);
                }

                fallbackRefreshTokens.put(memberId, rotatedRefreshToken);
                fallbackAuthMembers.put(memberId, authMember);

                return authMember;
            }
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

    private String serializeAuthMember(AuthMember authMember) {
        return authMember.memberId() + "\n" + authMember.email() + "\n" + authMember.role().name();
    }

    private AuthMember deserializeAuthMember(String value) {
        String[] parts = value.split("\n", 3);
        if (parts.length != 3) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        try {
            return new AuthMember(
                    Long.parseLong(parts[0]),
                    parts[1],
                    Role.valueOf(parts[2])
            );
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
}
