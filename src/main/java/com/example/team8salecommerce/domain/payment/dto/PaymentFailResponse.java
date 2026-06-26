package com.example.team8salecommerce.domain.payment.dto;

import java.time.LocalDateTime;

import com.example.team8salecommerce.domain.payment.entity.Payment;

/**
 * 결제 실패 처리 응답 DTO
 *
 * 결제 실패 처리 후 클라이언트에 반환할 값을 담는다.
 */
public record PaymentFailResponse(

	/**
	 * 결제 실패 처리된 주문 ID
	 */
	Long orderId,

	/**
	 * 저장된 결제 ID
	 */
	Long paymentId,

	/**
	 * PortOne 결제 ID
	 */
	String portOnePaymentId,

	/**
	 * 결제 실패 금액
	 */
	Long amount,

	/**
	 * 특가 주문 상태
	 *
	 * 정상 처리되면 PAYMENT_FAILED가 내려간다.
	 */
	String orderStatus,

	/**
	 * 결제 상태
	 *
	 * 정상 처리되면 FAILED가 내려간다.
	 */
	String paymentStatus,

	/**
	 * 결제 실패 시간
	 */
	LocalDateTime failedAt,

	/**
	 * 결제 실패 사유
	 */
	String failureReason
) {

	/**
	 * Payment 엔티티와 주문 상태를 응답 DTO로 변환한다.
	 */
	public static PaymentFailResponse of(Payment payment, String orderStatus) {
		return new PaymentFailResponse(
			payment.getOrderId(),
			payment.getId(),
			payment.getPortOnePaymentId(),
			payment.getAmount(),
			orderStatus,
			payment.getStatus().name(),
			payment.getFailedAt(),
			payment.getFailureReason()
		);
	}
}
