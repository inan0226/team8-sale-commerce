package com.example.team8salecommerce.domain.refund.client;

/**
 * PortOne 환불 요청 결과
 *
 * PortOne 결제 취소 API 응답 중
 * 현재 서비스에서 필요한 값만 담는 내부 DTO다.
 */
public record PortOneRefundResult(
	String cancellationId,
	String status
) {
	/**
	 * PortOne 환불이 성공 상태인지 확인한다.
	 *
	 * PortOne V2 결제 취소 완료 상태는 SUCCEEDED로 판단한다.
	 */
	public boolean isSucceeded() {
		return "SUCCEEDED".equals(status);
	}
}
