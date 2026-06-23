package com.example.team8salecommerce.domain.refund.entity;

/**
 * 환불 상태
 *
 * REFUND_REQUEST : 환불 요청이 생성된 상태
 * REFUNDED : 환불이 완료된 상태
 * REFUND_FAILED : 환불 처리에 실패한 상태
 */
public enum RefundStatus {

	REFUND_REQUEST,
	REFUNDED,
	REFUND_FAILED
}
