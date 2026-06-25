package com.example.team8salecommerce;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 애플리케이션 컨텍스트 로딩 테스트
 *
 * RedisLockManager는 RedissonClient를 필요로 한다.
 * 하지만 contextLoads 테스트에서 실제 Redis 서버까지 연결할 필요는 없으므로
 * RedissonClient를 MockitoBean으로 대체한다.
 */
@SpringBootTest
class Team8SaleCommerceApplicationTests {

	/**
	 * 테스트용 RedissonClient Mock Bean
	 *
	 * 실제 Redis 서버가 켜져 있지 않아도
	 * Spring Context가 정상적으로 로딩될 수 있게 한다.
	 */
	@MockitoBean
	private RedissonClient redissonClient;

	@Test
	void contextLoads() {
	}
}
