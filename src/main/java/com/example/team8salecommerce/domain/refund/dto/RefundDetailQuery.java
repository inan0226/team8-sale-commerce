package com.example.team8salecommerce.domain.refund.dto;

import java.time.LocalDateTime;

import com.example.team8salecommerce.domain.refund.entity.RefundStatus;

/**
 * 환불 상세 조회 전용 객체
 *
 * 환불 상세 조회 API에서 필요한 컬럼만 DB에서 직접 조회하기 위한 객체이다.
 * 엔티티 전체를 조회하지 않기 때문에 영속성 컨텍스트 부담을 줄일 수 있다.
 */
public record RefundDetailQuery(

	/**
	 * 환불 ID
	 */
	Long refundId,

	/**
	 * 환불 대상 주문 ID
	 */
	Long orderId,

	/**
	 * 환불 대상 결제 ID
	 */
	Long paymentId,

	/**
	 * 환불 금액
	 */
	Long refundAmount,

	/**
	 * 환불 상태
	 */
	RefundStatus refundStatus,

	/**
	 * 환불 요청 시간
	 */
	LocalDateTime requestedAt,

	/**
	 * 환불 완료 시간
	 */
	LocalDateTime completedAt
) {

	/**
	 * 조회 결과 객체를 API 응답 DTO로 변환한다.
	 *
	 * 상세 조회에서는 재고 복구 수량을 별도로 보여주지 않기 때문에
	 * restoredEventStock, remainingEventStock은 null로 둔다.
	 */
	public RefundResponse toResponse() {
		return new RefundResponse(
			refundId,
			orderId,
			paymentId,
			refundAmount,
			refundStatus.name(),
			null,
			null,
			requestedAt,
			completedAt
		);
	}
}
