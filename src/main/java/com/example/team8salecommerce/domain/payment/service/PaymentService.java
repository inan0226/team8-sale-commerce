package com.example.team8salecommerce.domain.payment.service;

import com.example.team8salecommerce.domain.payment.client.PortOnePaymentClient;
import com.example.team8salecommerce.domain.payment.client.PortOnePaymentInfo;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmResponse;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

/**
 * 결제 서비스
 *
 * PortOne 외부 결제 조회와 결제 승인 흐름의 진입점을 담당한다.
 *
 * 외부 API 호출은 DB row lock 밖에서 수행하고,
 * 실제 Payment 저장과 주문 PAID 변경은 PaymentConfirmTransactionService의
 * 짧은 트랜잭션 안에서 처리한다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PromotionOrderRepository promotionOrderRepository;
	private final PortOnePaymentClient portOnePaymentClient;
	private final PaymentConfirmTransactionService paymentConfirmTransactionService;

	/**
	 * 결제 승인 처리
	 *
	 * 1. 인증 회원 ID 검증
	 * 2. 주문 기본 조회
	 * 3. 주문 상태와 요청 금액 1차 검증
	 * 4. PortOne 외부 API로 실제 결제 정보 검증
	 * 5. 짧은 트랜잭션 안에서 주문 row lock 획득 후 Payment 저장 + 주문 PAID 변경
	 */
	public PaymentConfirmResponse confirmPayment(Long memberId, PaymentConfirmRequest request) {
		validateMemberId(memberId);

		PromotionOrder promotionOrder = findPromotionOrder(memberId, request.orderId());

		validatePromotionOrderCanBePaid(promotionOrder);
		validatePaymentAmount(promotionOrder, request.amount());

		PortOnePaymentInfo portOnePaymentInfo = validatePortOnePayment(
			promotionOrder,
			request.portOnePaymentId()
		);

		return paymentConfirmTransactionService.confirmPayment(
			memberId,
			request.orderId(),
			portOnePaymentInfo
		);
	}

	/**
	 * 인증된 회원 ID를 검증한다.
	 */
	private void validateMemberId(Long memberId) {
		if (memberId == null) {
			throw new CustomException(ErrorCode.UNAUTHORIZED);
		}
	}

	/**
	 * 주문을 일반 조회한다.
	 *
	 * 이 단계에서는 아직 row lock을 잡지 않는다.
	 * 외부 PortOne API 호출 전에 주문 존재 여부와 본인 주문 여부만 확인한다.
	 */
	private PromotionOrder findPromotionOrder(Long memberId, Long orderId) {
		return promotionOrderRepository.findByIdAndMemberId(orderId, memberId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_ORDER_NOT_FOUND));
	}

	/**
	 * 주문이 결제 가능한 상태인지 1차 검증한다.
	 *
	 * 실제 저장 직전에는 트랜잭션 서비스에서 row lock을 잡고 다시 검증한다.
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
	 * 주문 금액과 클라이언트 요청 금액이 일치하는지 1차 검증한다.
	 *
	 * 최종 결제 금액은 PortOne 조회 결과와 서버 주문 금액을 다시 비교한다.
	 */
	private void validatePaymentAmount(
		PromotionOrder promotionOrder,
		Long paymentAmount
	) {
		if (!promotionOrder.getTotalAmount().equals(paymentAmount)) {
			throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}
	}

	/**
	 * PortOne 서버의 실제 결제 정보를 검증한다.
	 *
	 * 클라이언트가 전달한 portOnePaymentId와 amount만 신뢰하지 않고,
	 * 서버가 PortOne 결제 단건 조회를 수행한 뒤 결제 ID, 상태, 금액을 검증한다.
	 */
	private PortOnePaymentInfo validatePortOnePayment(
		PromotionOrder promotionOrder,
		String portOnePaymentId
	) {
		PortOnePaymentInfo portOnePaymentInfo = portOnePaymentClient.getPayment(portOnePaymentId);

		if (!portOnePaymentId.equals(portOnePaymentInfo.paymentId())) {
			throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
		}

		if (!portOnePaymentInfo.isPaid()) {
			throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
		}

		if (!promotionOrder.getTotalAmount().equals(portOnePaymentInfo.totalAmount())) {
			throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}

		return portOnePaymentInfo;
	}
}
