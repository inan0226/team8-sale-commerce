package com.example.team8salecommerce.domain.refund.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.refund.entity.Refund;
import com.example.team8salecommerce.domain.refund.repository.RefundRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 환불 실패 트랜잭션 Service
 *
 * PortOne 환불 요청이 실패한 경우
 * 내부 환불 상태를 REFUND_FAILED로 변경하고,
 * 주문 상태를 다시 PAID로 되돌린다.
 */
@Service
@RequiredArgsConstructor
public class RefundFailTransactionService {

	private static final int MAX_FAILURE_REASON_LENGTH = 255;
	private static final String DEFAULT_FAILURE_REASON = "PortOne 환불 요청에 실패했습니다.";

	private final RefundRepository refundRepository;
	private final PromotionOrderRepository promotionOrderRepository;

	/**
	 * 환불 실패 처리
	 */
	@Transactional
	public void failRefund(RefundProcessingContext context, String failureReason) {
		validateContext(context);

		Refund refund = findRefundForUpdate(context.refundId());
		validateRefundRequested(refund);

		PromotionOrder promotionOrder = findPromotionOrderForUpdate(refund);

		LocalDateTime failedAt = LocalDateTime.now();

		refund.fail(failedAt, normalizeFailureReason(failureReason));

		promotionOrder.failRefundRequest();
	}

	/**
	 * 환불 처리 context를 검증한다.
	 */
	private void validateContext(RefundProcessingContext context) {
		if (context == null || context.refundId() == null) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}

	/**
	 * 환불 row를 조회하면서 쓰기 락을 획득한다.
	 */
	private Refund findRefundForUpdate(Long refundId) {
		return refundRepository.findByIdForUpdate(refundId)
			.orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));
	}

	/**
	 * 환불 요청 상태인지 검증한다.
	 */
	private void validateRefundRequested(Refund refund) {
		if (!refund.isRequested()) {
			throw new CustomException(ErrorCode.REFUND_NOT_ALLOWED);
		}
	}

	/**
	 * 환불 대상 주문을 조회하면서 쓰기 락을 획득한다.
	 */
	private PromotionOrder findPromotionOrderForUpdate(Refund refund) {
		return promotionOrderRepository.findByIdAndMemberIdForUpdate(refund.getOrderId(), refund.getMemberId())
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_ORDER_NOT_FOUND));
	}

	/**
	 * 실패 사유를 DB 컬럼 길이에 맞게 정리한다.
	 */
	private String normalizeFailureReason(String failureReason) {
		String normalizedReason = DEFAULT_FAILURE_REASON;

		if (StringUtils.hasText(failureReason)) {
			normalizedReason = failureReason;
		}

		if (normalizedReason.length() <= MAX_FAILURE_REASON_LENGTH) {
			return normalizedReason;
		}

		return normalizedReason.substring(0, MAX_FAILURE_REASON_LENGTH);
	}
}
