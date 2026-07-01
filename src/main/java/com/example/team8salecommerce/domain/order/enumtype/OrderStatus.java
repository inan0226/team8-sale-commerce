package com.example.team8salecommerce.domain.order.enumtype;

// 일반 주문과 특가 주문이 공유하는 처리 상태

public enum OrderStatus {

	// 결제 대기 상태
	WAITING,

	// 결제 완료 상태
	PAID,

	// 결제 실패 상태
	PAYMENT_FAILED,

	// 환불 요청 상태
	REFUND_REQUEST,

	// 환불 완료 상태
	REFUNDED,

	// 주문 취소 상태
	CANCELLED
}