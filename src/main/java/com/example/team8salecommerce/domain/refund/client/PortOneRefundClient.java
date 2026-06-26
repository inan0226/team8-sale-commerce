package com.example.team8salecommerce.domain.refund.client;

/**
 * PortOne 환불 Client
 *
 * 서버가 환불 요청을 접수한 뒤,
 * 실제 PortOne 결제 취소 API를 호출하는 역할을 담당한다.
 */
public interface PortOneRefundClient {
	/**
	 * PortOne 결제 ID 기준으로 결제 취소를 요청한다.
	 *
	 * @param portOnePaymentId PortOne 결제 ID
	 * @param amount 환불 금액
	 * @param reason 환불 사유
	 * @return PortOne 환불 요청 결과
	 */
	PortOneRefundResult refund(
		String portOnePaymentId,
		Long amount,
		String reason
	);
}
