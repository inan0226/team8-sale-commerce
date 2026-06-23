package com.example.team8salecommerce.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 설정 클래스
 *
 * Redis Lock을 사용하기 위해 RedissonClient를 Spring Bean으로 등록한다.
 *
 * RedisLockManager에서 RedissonClient를 주입받기 때문에
 * 이 설정이 없으면 애플리케이션 실행 또는 테스트 시 Bean 생성 오류가 발생한다.
 */
@Configuration
public class RedissonConfig {

	/**
	 * Redis host
	 *
	 * application.properties에 spring.data.redis.host 값이 있으면 그 값을 사용하고,
	 * 없으면 기본값 localhost를 사용한다.
	 */
	@Value("${spring.data.redis.host:localhost}")
	private String redisHost;

	/**
	 * Redis port
	 *
	 * application.properties에 spring.data.redis.port 값이 있으면 그 값을 사용하고,
	 * 없으면 기본값 6379를 사용한다.
	 */
	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	/**
	 * RedissonClient Bean 등록
	 *
	 * Redis 단일 서버 모드로 연결한다.
	 * 생성된 RedissonClient는 Redis Lock 획득/해제에 사용된다.
	 */
	@Bean(destroyMethod = "shutdown")
	public RedissonClient redissonClient() {
		Config config = new Config();

		config.useSingleServer()
			.setAddress("redis://" + redisHost + ":" + redisPort);

		return Redisson.create(config);
	}
}
