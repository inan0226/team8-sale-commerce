package com.example.team8salecommerce.domain.refund.entity;

/**
 * 환불 사유 타입
 *
 * CHANGE_OF_MIND : 단순 변심
 * PAYMENT_ERROR : 결제 오류
 * PRODUCT_ISSUE : 상품 문제
 * ETC : 기타 사유
 *
 * 배송 기능은 이번 범위에서 제외했으므로
 * 배송 관련 환불 사유는 넣지 않는다.
 */
public enum RefundReasonType {

	CHANGE_OF_MIND,
	PAYMENT_ERROR,
	PRODUCT_ISSUE,
	ETC
}
