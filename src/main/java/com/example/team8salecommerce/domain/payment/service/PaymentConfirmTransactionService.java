package com.example.team8salecommerce.domain.payment.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.team8salecommerce.domain.payment.client.PortOnePaymentInfo;
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
 * 결제 승인 트랜잭션 서비스
 *
 * DB row lock이 필요한 짧은 구간만 트랜잭션으로 처리한다.
 * PortOne 외부 API 호출은 이 서비스에 들어오기 전에 끝난 상태여야 한다.
 */
@Service
@RequiredArgsConstructor
public class PaymentConfirmTransactionService {

	private static final String PORT_ONE_PAYMENT_ID_UNIQUE_CONSTRAINT = "uk_payment_portone_payment_id";
	private static final String PORT_ONE_PAYMENT_ID_COLUMN = "port_one_payment_id";

	private final PaymentRepository paymentRepository;
	private final PromotionOrderRepository promotionOrderRepository;

	/**
	 * 결제 승인 정보를 저장하고 주문 상태를 PAID로 변경한다.
	 *
	 * 같은 주문에 대한 동시 승인 요청을 막기 위해
	 * 주문 row lock을 획득한 뒤 주문 상태와 결제 중복 여부를 다시 검증한다.
	 */
	@Transactional
	public PaymentConfirmResponse confirmPayment(
		Long memberId,
		Long orderId,
		PortOnePaymentInfo portOnePaymentInfo
	) {
		PromotionOrder promotionOrder = findPromotionOrderForUpdate(memberId, orderId);

		validatePromotionOrderCanBePaid(promotionOrder);
		validateAlreadyPaidPayment(orderId);
		validateDuplicatedPortOnePayment(portOnePaymentInfo.paymentId());
		validatePaymentAmount(promotionOrder, portOnePaymentInfo.totalAmount());

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
	 * 주문을 조회하면서 쓰기 락을 획득한다.
	 *
	 * 같은 주문에 서로 다른 portOnePaymentId로 동시 승인 요청이 들어와도
	 * 한 요청만 먼저 처리되고, 이후 요청은 변경된 주문 상태를 보고 실패한다.
	 */
	private PromotionOrder findPromotionOrderForUpdate(Long memberId, Long orderId) {
		return promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROMOTION_ORDER_NOT_FOUND));
	}

	/**
	 * 주문 상태를 재검증한다.
	 *
	 * PortOne 조회 이후 다른 요청이 먼저 주문을 PAID로 바꿨을 수 있으므로
	 * row lock을 잡은 뒤 반드시 다시 확인한다.
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
	 */
	private void validateDuplicatedPortOnePayment(String portOnePaymentId) {
		boolean duplicated = paymentRepository.findByPortOnePaymentId(portOnePaymentId)
			.isPresent();

		if (duplicated) {
			throw new CustomException(ErrorCode.DUPLICATED_PAYMENT);
		}
	}

	/**
	 * PortOne 결제 금액과 서버 주문 금액이 일치하는지 최종 검증한다.
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
	 * 결제 승인 정보를 저장한다.
	 *
	 * portOnePaymentId unique 제약 위반인 경우에만 DUPLICATED_PAYMENT로 변환하고,
	 * 그 외 DB 제약 오류는 PAYMENT_CONFIRM_FAILED로 처리한다.
	 */
	private Payment savePaidPayment(Payment payment) {
		try {
			return paymentRepository.saveAndFlush(payment);
		} catch (DataIntegrityViolationException e) {
			if (isPortOnePaymentIdUniqueViolation(e)) {
				throw new CustomException(ErrorCode.DUPLICATED_PAYMENT);
			}

			throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
		}
	}

	/**
	 * DB 제약 오류가 portOnePaymentId unique 제약 위반인지 확인한다.
	 */
	private boolean isPortOnePaymentIdUniqueViolation(DataIntegrityViolationException exception) {
		Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(exception);
		String message = rootCause.getMessage();

		if (!StringUtils.hasText(message)) {
			return false;
		}

		String lowerCaseMessage = message.toLowerCase(Locale.ROOT);

		return lowerCaseMessage.contains(PORT_ONE_PAYMENT_ID_UNIQUE_CONSTRAINT)
			|| lowerCaseMessage.contains(PORT_ONE_PAYMENT_ID_COLUMN);
	}
}
