package com.example.team8salecommerce.domain.payment.client;

/**
 * PortOne 결제 조회 결과
 *
 * PaymentService에서 필요한 값만 사용하기 위해 만든 내부 DTO다.
 */
public record PortOnePaymentInfo(
	String paymentId,
	String status,
	Long totalAmount
) {

	/**
	 * PortOne 결제가 결제 완료 상태인지 확인한다.
	 */
	public boolean isPaid() {
		return "PAID".equals(status);
	}
}
