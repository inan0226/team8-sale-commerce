package com.example.team8salecommerce.domain.refund.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.refund.dto.RefundRequest;
import com.example.team8salecommerce.domain.refund.entity.Refund;
import com.example.team8salecommerce.domain.refund.entity.RefundStatus;
import com.example.team8salecommerce.domain.refund.repository.RefundRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 환불 요청 트랜잭션 Service
 *
 * 환불 처리의 첫 번째 단계다.
 *
 * 이 단계에서는 외부 PortOne API를 호출하지 않는다.
 * DB row lock을 잡은 상태에서는 내부 DB 상태만 빠르게 변경하고,
 * 외부 API 호출은 Facade에서 트랜잭션 밖에서 수행한다.
 */
@Service
@RequiredArgsConstructor
public class RefundRequestTransactionService {

	private final RefundRepository refundRepository;
	private final PaymentRepository paymentRepository;
	private final PromotionOrderRepository promotionOrderRepository;

	/**
	 * 환불 요청을 생성하고 주문 상태를 REFUND_REQUEST로 변경한다.
	 *
	 * 같은 주문에 환불 요청이 동시에 들어오는 경우를 막기 위해
	 * PromotionOrder row lock을 획득한 뒤 처리한다.
	 */
	@Transactional
	public RefundProcessingContext requestRefund(
		Long memberId,
		Long orderId,
		RefundRequest request
	) {
		validateMemberId(memberId);
		validateOrderId(orderId);

		PromotionOrder promotionOrder = findPromotionOrderForUpdate(memberId, orderId);

		validatePromotionOrderCanBeRefunded(promotionOrder);
		validateAlreadyRefundRequested(orderId);
		validateAlreadyRefunded(orderId);

		Payment payment = findPaidPayment(orderId);

		LocalDateTime requestedAt = LocalDateTime.now();

		Refund refund = Refund.createRequest(
			promotionOrder.getId(),
			payment.getId(),
			memberId,
			request.reasonType(),
			request.reasonDetail(),
			payment.getAmount(),
			requestedAt
		);

		Refund savedRefund = refundRepository.saveAndFlush(refund);

		promotionOrder.requestRefund(requestedAt);

		return new RefundProcessingContext(
			savedRefund.getId(),
			promotionOrder.getId(),
			payment.getId(),
			memberId,
			payment.getPortOnePaymentId(),
			payment.getAmount(),
			request.reasonType(),
			request.reasonDetail()
		);
	}

	/**
	 * 인증 회원 ID를 검증한다.
	 */
	private void validateMemberId(Long memberId) {
		if (memberId == null) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}

	/**
	 * 주문 ID를 검증한다.
	 */
	private void validateOrderId(Long orderId) {
		if (orderId == null || orderId <= 0) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}

	/**
	 * 본인 특가 주문을 조회하면서 쓰기 락을 획득한다.
	 */
	private PromotionOrder findPromotionOrderForUpdate(Long memberId, Long orderId) {
		return promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_ORDER_NOT_FOUND));
	}

	/**
	 * 환불 가능한 주문 상태인지 검증한다.
	 *
	 * 결제 완료 상태인 주문만 환불 요청할 수 있다.
	 */
	private void validatePromotionOrderCanBeRefunded(PromotionOrder promotionOrder) {
		if (!promotionOrder.isPaid()) {
			throw new CustomException(ErrorCode.REFUND_NOT_ALLOWED);
		}
	}

	/**
	 * 이미 환불 요청된 주문인지 검증한다.
	 */
	private void validateAlreadyRefundRequested(Long orderId) {
		boolean alreadyRequested = refundRepository.existsByOrderIdAndStatus(orderId, RefundStatus.REFUND_REQUEST);

		if (alreadyRequested) {
			throw new CustomException(ErrorCode.REFUND_ALREADY_REQUESTED);
		}
	}

	/**
	 * 이미 환불 완료된 주문인지 검증한다.
	 */
	private void validateAlreadyRefunded(Long orderId) {
		boolean alreadyRefunded = refundRepository.existsByOrderIdAndStatus(orderId, RefundStatus.REFUNDED);

		if (alreadyRefunded) {
			throw new CustomException(ErrorCode.ALREADY_REFUNDED);
		}
	}

	/**
	 * 결제 완료 Payment를 조회한다.
	 *
	 * 실패 Payment가 아니라 실제 결제 완료된 Payment만 환불 대상으로 사용한다.
	 */
	private Payment findPaidPayment(Long orderId) {
		return paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PAID)
			.orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
	}
}
