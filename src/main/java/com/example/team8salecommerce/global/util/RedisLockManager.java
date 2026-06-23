package com.example.team8salecommerce.global.util;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * Redis Lock 공통 유틸
 *
 * 선착순 이벤트처럼 동시에 많은 요청이 들어오는 기능에서
 * 특정 자원에 대한 동시 접근을 제어하기 위해 사용한다.
 *
 * 현재 주 사용 대상:
 * - 선착순 구매 시 이벤트 재고 차감
 * - 결제 실패 시 이벤트 재고 복구
 * - 환불 성공 시 이벤트 재고 복구
 */
@Component
@RequiredArgsConstructor
public class RedisLockManager {

	/**
	 * 특가 상품 Lock Key prefix
	 *
	 * promotionProductId 기준으로 Lock을 분리한다.
	 * 예: lock:promotion-product:10
	 */
	private static final String PROMOTION_PRODUCT_LOCK_KEY_PREFIX = "lock:promotion-product:";

	/**
	 * Lock 획득을 기다리는 최대 시간
	 *
	 * 3초 동안 Lock을 얻지 못하면 요청 실패로 처리한다.
	 */
	private static final long DEFAULT_WAIT_TIME = 3L;

	/**
	 * Lock 자동 해제 시간
	 *
	 * 예상치 못한 오류로 unlock이 호출되지 않아도
	 * 5초 뒤에는 Redis Lock이 자동으로 해제된다.
	 */
	private static final long DEFAULT_LEASE_TIME = 5L;

	private final RedissonClient redissonClient;

	/**
	 * 특가 상품 전용 Lock Key를 생성한다.
	 *
	 * 구매, 결제 실패, 환불에서 모두 같은 규칙을 사용해야
	 * 재고 차감과 복구가 동시에 일어나도 꼬이지 않는다.
	 */
	public String createPromotionProductLockKey(Long promotionProductId) {
		if (promotionProductId == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}

		return PROMOTION_PRODUCT_LOCK_KEY_PREFIX + promotionProductId;
	}

	/**
	 * 반환값이 있는 로직을 Redis Lock으로 감싸서 실행한다.
	 *
	 * 예:
	 * - 선착순 구매 처리 후 구매 응답 DTO 반환
	 * - 환불 처리 후 환불 응답 DTO 반환
	 */
	public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
		RLock lock = redissonClient.getLock(lockKey);
		boolean isLocked = false;

		try {
			isLocked = lock.tryLock(
				DEFAULT_WAIT_TIME,
				DEFAULT_LEASE_TIME,
				TimeUnit.SECONDS
			);

			if (!isLocked) {
				throw new CustomException(ErrorCode.LOCK_ACQUIRE_FAILED);
			}

			return supplier.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CustomException(ErrorCode.LOCK_ACQUIRE_FAILED);
		} finally {
			unlock(lock, isLocked);
		}
	}

	/**
	 * 반환값이 없는 로직을 Redis Lock으로 감싸서 실행한다.
	 *
	 * 예:
	 * - 단순 상태 변경
	 * - 재고 복구만 수행하는 로직
	 */
	public void executeWithLock(String lockKey, Runnable runnable) {
		executeWithLock(lockKey, () -> {
			runnable.run();
			return null;
		});
	}

	/**
	 * Lock 해제 처리
	 *
	 * 반드시 현재 스레드가 Lock을 가지고 있는 경우에만 해제한다.
	 * 그렇지 않으면 다른 스레드가 가진 Lock을 잘못 해제할 수 있다.
	 */
	private void unlock(RLock lock, boolean isLocked) {
		if (isLocked && lock.isHeldByCurrentThread()) {
			lock.unlock();
		}
	}
}
