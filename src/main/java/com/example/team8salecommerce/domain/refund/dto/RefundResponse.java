package com.example.team8salecommerce.domain.refund.dto;

import java.time.LocalDateTime;

import com.example.team8salecommerce.domain.refund.entity.Refund;

/**
 * 환불 요청 응답 DTO
 *
 * 환불 요청 또는 환불 완료 후 클라이언트에게 반환할 데이터이다.
 * DTO는 팀 컨벤션에 따라 record를 사용한다.
 */
public record RefundResponse(

	Long refundId,
	Long orderId,
	Long paymentId,
	Long refundAmount,
	String refundStatus,

	/**
	 * 복구된 이벤트 재고 수량
	 */
	Integer restoredEventStock,

	/**
	 * 복구 후 남은 이벤트 재고
	 */
	Integer remainingEventStock,
	LocalDateTime requestedAt,
	LocalDateTime completedAt
) {

	/**
	 * 환불 엔티티를 환불 응답 DTO로 변환한다.
	 *
	 * 아직 재고 복구 정보가 없는 경우에는
	 * restoredEventStock, remainingEventStock에 null을 넣는다.
	 */
	public static RefundResponse from(Refund refund) {
		return new RefundResponse(
			refund.getId(),
			refund.getOrderId(),
			refund.getPaymentId(),
			refund.getRefundAmount(),
			refund.getStatus().name(),
			null,
			null,
			refund.getRequestedAt(),
			refund.getCompletedAt()
		);
	}

	/**
	 * 환불 엔티티와 재고 복구 정보를 함께 응답 DTO로 변환한다.
	 *
	 * 환불 성공 후 이벤트 재고 복구까지 완료된 경우 사용할 수 있다.
	 */
	public static RefundResponse of(
		Refund refund,
		Integer restoredEventStock,
		Integer remainingEventStock
	) {
		return new RefundResponse(
			refund.getId(),
			refund.getOrderId(),
			refund.getPaymentId(),
			refund.getRefundAmount(),
			refund.getStatus().name(),
			restoredEventStock,
			remainingEventStock,
			refund.getRequestedAt(),
			refund.getCompletedAt()
		);
	}
}
