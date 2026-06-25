package com.example.team8salecommerce.domain.promotion.facade;

import org.springframework.stereotype.Component;

import com.example.team8salecommerce.domain.promotion.dto.PromotionPurchaseRequest;
import com.example.team8salecommerce.domain.promotion.dto.PromotionPurchaseResponse;
import com.example.team8salecommerce.domain.promotion.service.PromotionPurchaseService;
import com.example.team8salecommerce.global.util.RedisLockManager;

import lombok.RequiredArgsConstructor;

/**
 * 선착순 특가 구매 Facade
 *
 * 선착순 구매의 전체 흐름을 조율한다.
 *
 * Facade의 역할:
 * - Redis Lock Key 생성
 * - Redis Lock 획득
 * - 실제 구매 Service 호출
 *
 * 실제 DB 변경 로직은 PromotionPurchaseService에서 처리한다.
 */
@Component
@RequiredArgsConstructor
public class PromotionPurchaseFacade {

	private final RedisLockManager redisLockManager;
	private final PromotionPurchaseService promotionPurchaseService;

	/**
	 * 선착순 특가 상품을 구매한다.
	 *
	 * promotionProductId 기준으로 Redis Lock을 걸어
	 * 동시에 여러 사용자가 같은 특가 상품을 구매해도
	 * 이벤트 재고가 음수가 되지 않도록 막는다.
	 */
	public PromotionPurchaseResponse purchase(
		Long memberId,
		Long promotionProductId,
		PromotionPurchaseRequest request
	) {
		String lockKey = redisLockManager.createPromotionProductLockKey(promotionProductId);

		return redisLockManager.executeWithLock(lockKey, () ->
			promotionPurchaseService.purchaseWithTransaction(
				memberId,
				promotionProductId,
				request
			)
		);
	}
}
