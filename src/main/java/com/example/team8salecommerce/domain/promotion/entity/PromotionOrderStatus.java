package com.example.team8salecommerce.domain.promotion.entity;

/**
 * 특가 주문 상태
 *
 * 선착순 특가 구매는 일반 장바구니 주문을 경유하지 않기 때문에
 * 특가 주문 전용 상태를 따로 관리한다.
 */
public enum PromotionOrderStatus {

	WAITING,
	PAID,
	PAYMENT_FAILED,
	REFUND_REQUEST,
	REFUNDED
}
