package com.example.team8salecommerce.domain.payment.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team8salecommerce.domain.payment.client.PortOnePaymentClient;
import com.example.team8salecommerce.domain.payment.client.PortOnePaymentInfo;
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
	private final PortOnePaymentClient portOnePaymentClient;

	/**
	 * 결제 승인 처리
	 *
	 * 사용자가 PortOne 결제를 완료한 뒤 서버에 승인 요청을 보내면,
	 * 서버는 주문 상태, 결제 금액, PortOne 실제 결제 정보를 검증한 뒤
	 * 결제 정보를 저장하고 특가 주문 상태를 PAID로 변경한다.
	 */
	@Transactional
	public PaymentConfirmResponse confirmPayment(Long memberId, PaymentConfirmRequest request) {
		validateMemberId(memberId);

		PromotionOrder promotionOrder = findPromotionOrder(memberId, request.orderId());

		validatePromotionOrderCanBePaid(promotionOrder);
		validateAlreadyPaidPayment(request.orderId());
		validateDuplicatedPortOnePayment(request.portOnePaymentId());
		validatePaymentAmount(promotionOrder, request.amount());

		PortOnePaymentInfo portOnePaymentInfo = validatePortOnePayment(
			promotionOrder,
			request.portOnePaymentId()
		);

		LocalDateTime paidAt = LocalDateTime.now();

		Payment payment = Payment.createPaidPayment(
			promotionOrder.getId(),
			portOnePaymentInfo.paymentId(),
			portOnePaymentInfo.totalAmount(),
			null,
			paidAt
		);

		Payment savedPayment = savePaidPayment(payment);

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
	 * 결제 요청한 주문을 조회하면서 쓰기 락을 획득한다.
	 *
	 * findByIdAndMemberIdForUpdate를 사용해서 주문 존재 여부와 본인 주문 여부를 함께 검증하고,
	 * 같은 주문에 대한 결제 승인 요청이 동시에 처리되지 않도록 방어한다.
	 */
	private PromotionOrder findPromotionOrder(Long memberId, Long orderId) {
		return promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
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
		boolean alreadyPaid = paymentRepository.existsByOrderIdAndStatus(
			orderId,
			PaymentStatus.PAID
		);

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
		boolean duplicated = paymentRepository.findByPortOnePaymentId(portOnePaymentId)
			.isPresent();

		if (duplicated) {
			throw new CustomException(ErrorCode.DUPLICATED_PAYMENT);
		}
	}

	/**
	 * 주문 금액과 클라이언트 요청 금액이 일치하는지 검증한다.
	 *
	 * 클라이언트가 보낸 금액만으로 결제를 확정하지는 않지만,
	 * 요청값이 서버 주문 금액과 다른 경우는 잘못된 요청으로 먼저 차단한다.
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
	 * 클라이언트가 전달한 portOnePaymentId와 amount만 신뢰하면
	 * 임의의 결제 ID와 정상 주문 금액만으로 주문이 PAID 처리될 수 있다.
	 *
	 * 따라서 서버가 PortOne 결제 단건 조회를 수행하고,
	 * 결제 ID, 결제 상태, 결제 금액을 서버 주문 정보와 비교한다.
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

	/**
	 * 결제 승인 정보를 저장한다.
	 *
	 * 애플리케이션 레벨에서 PortOne 결제 ID 중복을 먼저 검증하지만,
	 * 동시에 같은 PortOne 결제 ID로 요청이 들어오면 두 요청이 모두 사전 검증을 통과할 수 있다.
	 *
	 * 따라서 DB unique 제약을 최종 방어선으로 두고,
	 * unique 제약 위반이 발생하면 공통 결제 중복 예외로 변환한다.
	 */
	private Payment savePaidPayment(Payment payment) {
		try {
			return paymentRepository.saveAndFlush(payment);
		} catch (DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.DUPLICATED_PAYMENT);
		}
	}
}
