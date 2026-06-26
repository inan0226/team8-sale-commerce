package com.example.team8salecommerce.domain.refund.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team8salecommerce.domain.refund.dto.RefundResponse;
import com.example.team8salecommerce.domain.refund.entity.Refund;
import com.example.team8salecommerce.domain.refund.repository.RefundRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 환불 조회 Service
 *
 * 환불 상세 조회처럼 데이터를 변경하지 않고
 * 조회만 필요한 기능을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundQueryService {

	private final RefundRepository refundRepository;

	/**
	 * 로그인한 회원의 환불 상세 정보를 조회한다.
	 *
	 * @param memberId 로그인한 회원 ID
	 * @param refundId 조회할 환불 ID
	 * @return 환불 상세 응답
	 */
	public RefundResponse getRefund(Long memberId, Long refundId) {
		validateMemberId(memberId);
		validateRefundId(refundId);

		Refund refund = refundRepository.findByIdAndMemberId(refundId, memberId)
			.orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));

		return RefundResponse.from(refund);
	}

	/**
	 * 로그인 회원 ID를 검증한다.
	 */
	private void validateMemberId(Long memberId) {
		if (memberId == null) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}

	/**
	 * 환불 ID를 검증한다.
	 */
	private void validateRefundId(Long refundId) {
		if (refundId == null || refundId <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}
}
