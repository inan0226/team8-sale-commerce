package com.example.team8salecommerce.domain.payment.dto;

import java.time.LocalDateTime;

import com.example.team8salecommerce.domain.payment.entity.Payment;

/**
 * 결제 승인 응답 DTO
 *
 * 결제 승인 성공 후 클라이언트에게 반환할 데이터이다.
 * DTO는 팀 컨벤션에 따라 record를 사용한다.
 */
public record PaymentConfirmResponse(

	Long orderId,
	Long paymentId,
	String portOnePaymentId,
	Long amount,
	String orderStatus,
	String paymentStatus,
	LocalDateTime paidAt
) {

	/**
	 * 결제 엔티티를 결제 승인 응답 DTO로 변환한다.
	 *
	 * Controller에서 Entity를 직접 반환하지 않고,
	 * 필요한 값만 DTO로 변환해서 반환한다.
	 */
	public static PaymentConfirmResponse of(Payment payment, String orderStatus) {
		return new PaymentConfirmResponse(
			payment.getOrderId(),
			payment.getId(),
			payment.getPortOnePaymentId(),
			payment.getAmount(),
			orderStatus,
			payment.getStatus().name(),
			payment.getPaidAt()
		);
	}
}
