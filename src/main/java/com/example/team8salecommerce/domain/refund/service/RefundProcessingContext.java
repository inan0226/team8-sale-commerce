package com.example.team8salecommerce.domain.refund.service;

import com.example.team8salecommerce.domain.refund.entity.RefundReasonType;

/**
 * 환불 처리 흐름에서 단계 간 전달할 값
 *
 * 환불 요청 저장 트랜잭션 이후,
 * PortOne 환불 요청과 환불 완료/실패 트랜잭션에서
 * 공통으로 필요한 값을 담는다.
 */
public record RefundProcessingContext(
	Long refundId,
	Long orderId,
	Long paymentId,
	Long memberId,
	String portOnePaymentId,
	Long refundAmount,
	RefundReasonType reasonType,
	String reasonDetail
) {
}
