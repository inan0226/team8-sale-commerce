package com.example.team8salecommerce.domain.refund.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team8salecommerce.domain.refund.client.PortOneRefundResult;
import com.example.team8salecommerce.domain.refund.entity.Refund;
import com.example.team8salecommerce.domain.refund.repository.RefundRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * PortOne 환불 성공 정보 저장 트랜잭션 Service
 *
 * PortOne 환불 API가 성공한 직후,
 * 내부 환불 완료 처리 전에 PortOne 취소 성공 정보를 DB에 먼저 저장한다.
 *
 * 이렇게 하면 이후 재고 복구나 재고 이력 저장 중 문제가 생겨도
 * 실제 외부 환불은 성공했다는 정보를 DB에 남길 수 있다.
 */
@Service
@RequiredArgsConstructor
public class RefundPortOneSuccessTransactionService {

	private final RefundRepository refundRepository;

	/**
	 * PortOne 환불 성공 정보를 저장한다.
	 */
	@Transactional
	public void recordPortOneRefundSuccess(
		RefundProcessingContext context,
		PortOneRefundResult refundResult
	) {
		validateContext(context);
		validateRefundResult(refundResult);

		Refund refund = refundRepository.findByIdForUpdate(context.refundId())
			.orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));

		refund.recordPortOneRefundSuccess(
			refundResult.cancellationId(),
			refundResult.status(),
			LocalDateTime.now()
		);
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
	 * PortOne 환불 성공 결과를 검증한다.
	 */
	private void validateRefundResult(PortOneRefundResult refundResult) {
		if (
			refundResult == null
				|| refundResult.cancellationId() == null
				|| refundResult.cancellationId().isBlank()
				|| refundResult.status() == null
				|| refundResult.status().isBlank()
		) {
			throw new CustomException(ErrorCode.REFUND_FAILED);
		}
	}
}
