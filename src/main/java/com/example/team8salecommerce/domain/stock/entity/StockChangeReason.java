package com.example.team8salecommerce.domain.stock.entity;

/**
 * 재고 변경 사유
 *
 * PROMOTION_PURCHASE : 선착순 구매 성공으로 재고 차감
 * PAYMENT_FAILED : 결제 실패로 재고 복구
 * REFUND_COMPLETED : 환불 완료로 재고 복구
 */
public enum StockChangeReason {

	PROMOTION_PURCHASE,
	PAYMENT_FAILED,
	REFUND_COMPLETED
}
