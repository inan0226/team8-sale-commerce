package com.example.team8salecommerce.domain.payment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmResponse;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 결제 서비스
 *
 * 특가 주문 결제 승인, 결제 실패 처리 등
 * 결제 도메인의 핵심 비즈니스 로직을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final PromotionOrderRepository promotionOrderRepository;

	/**
	 * 결제 승인 처리
	 *
	 * 사용자가 PortOne 결제를 완료한 뒤 서버에 승인 요청을 보내면,
	 * 서버는 주문 상태와 결제 금액을 검증한 뒤 결제 정보를 저장하고
	 * 특가 주문 상태를 PAID로 변경한다.
	 */
	@Transactional
	public PaymentConfirmResponse confirmPayment(Long memberId, PaymentConfirmRequest request) {
		validateMemberId(memberId);

		PromotionOrder promotionOrder = findPromotionOrder(memberId, request.orderId());

		validatePromotionOrderCanBePaid(promotionOrder);
		validateAlreadyPaidPayment(request.orderId());
		validateDuplicatedPortOnePayment(request.portOnePaymentId());
		validatePaymentAmount(promotionOrder, request.amount());

		LocalDateTime paidAt = LocalDateTime.now();

		Payment payment = Payment.createPaidPayment(
			promotionOrder.getId(),
			request.portOnePaymentId(),
			request.amount(),
			null,
			paidAt
		);

		Payment savedPayment = paymentRepository.save(payment);

		promotionOrder.markAsPaid(paidAt);

		return PaymentConfirmResponse.of(savedPayment, promotionOrder.getStatus().name());
	}

	/**
	 * 인증된 회원 ID를 검증한다.
	 *
	 * Controller에서 AuthMember null 방어를 하더라도,
	 * Service에서도 한 번 더 방어하면 테스트와 내부 호출에서도 안전하다.
	 */
	private void validateMemberId(Long memberId) {
		if (memberId == null) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}

	/**
	 * 결제 요청한 주문을 조회한다.
	 *
	 * findByIdAndMemberId를 사용해서
	 * 주문 존재 여부와 본인 주문 여부를 함께 검증한다.
	 */
	private PromotionOrder findPromotionOrder(Long memberID, Long orderID) {
		return promotionOrderRepository.findByIdAndMemberId(orderID, memberID)
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_ORDER_NOT_FOUND));
	}

	/**
	 * 특가 주문이 결제 가능한 상태인지 검증한다.
	 *
	 * 이미 PAID 상태라면 중복 결제 예외를 던지고,
	 * WAITING이 아닌 다른 상태라면 주문 상태 오류로 처리한다.
	 */
	private void validatePromotionOrderCanBePaid(PromotionOrder promotionOrder) {
		if (promotionOrder.isPaid()) {
			throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
		}
		if (!promotionOrder.isWaiting()) {
			throw new CustomException(ErrorCode.INVALID_PROMOTION_ORDER_STATUS);
		}
	}

	/**
	 * 같은 주문에 이미 결제 완료 내역이 있는지 확인한다.
	 *
	 * 주문 상태와 별개로 Payment 테이블 기준에서도
	 * 중복 결제를 한 번 더 방어한다.
	 */
	private void validateAlreadyPaidPayment(Long orderId) {
		boolean alreadyPaid = paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PAID);

		if (alreadyPaid) {
			throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
		}
	}

	/**
	 * 같은 PortOne 결제 ID가 이미 저장되어 있는지 확인한다.
	 *
	 * 외부 결제 ID는 중복되면 안 되므로,
	 * DB unique 제약에 걸리기 전에 비즈니스 예외로 먼저 처리한다.
	 */
	private void validateDuplicatedPortOnePayment(String portOnePaymentId) {
		boolean duplicated = paymentRepository.findByPortOnePaymentId(portOnePaymentId).isPresent();

		if (duplicated) {
			throw new CustomException(ErrorCode.DUPLICATED_PAYMENT);
		}
	}

	/**
	 * 주문 금액과 결제 요청 금액이 일치하는지 검증한다.
	 *
	 * 클라이언트가 보낸 금액을 그대로 믿지 않고,
	 * 서버에 저장된 주문 금액과 반드시 비교한다.
	 */
	private void validatePaymentAmount(
		PromotionOrder promotionOrder,
		Long paymentAmount
	) {
		if (!promotionOrder.getTotalAmount().equals(paymentAmount)) {
			throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}
	}
}
