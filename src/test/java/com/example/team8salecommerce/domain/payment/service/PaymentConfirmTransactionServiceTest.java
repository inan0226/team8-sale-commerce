package com.example.team8salecommerce.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.team8salecommerce.domain.payment.client.PortOnePaymentInfo;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmResponse;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**
 * 결제 승인 트랜잭션 서비스 단위 테스트
 *
 * 주문 row lock 이후 결제 저장과 주문 PAID 변경 흐름을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentConfirmTransactionServiceTest {

	private static final Long MEMBER_ID = 1L;
	private static final Long ORDER_ID = 10L;
	private static final Long PROMOTION_PRODUCT_ID = 100L;
	private static final Long PAYMENT_ID = 1000L;
	private static final String PORT_ONE_PAYMENT_ID = "payment-123";
	private static final Long ORDER_AMOUNT = 14000L;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PromotionOrderRepository promotionOrderRepository;

	@InjectMocks
	private PaymentConfirmTransactionService paymentConfirmTransactionService;

	@Test
	@DisplayName("주문 row lock 획득 후 결제 승인에 성공한다")
	void confirmPaymentSuccess() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PortOnePaymentInfo portOnePaymentInfo = createPaidPortOnePaymentInfo(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());
		when(paymentRepository.saveAndFlush(any(Payment.class)))
			.thenAnswer(invocation -> {
				Payment payment = invocation.getArgument(0);

				ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);

				return payment;
			});

		// when
		PaymentConfirmResponse response = paymentConfirmTransactionService.confirmPayment(
			MEMBER_ID,
			ORDER_ID,
			portOnePaymentInfo
		);

		// then
		assertThat(response.orderId()).isEqualTo(ORDER_ID);
		assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
		assertThat(response.portOnePaymentId()).isEqualTo(PORT_ONE_PAYMENT_ID);
		assertThat(response.amount()).isEqualTo(ORDER_AMOUNT);
		assertThat(response.orderStatus()).isEqualTo("PAID");
		assertThat(response.paymentStatus()).isEqualTo("PAID");
		assertThat(response.paidAt()).isNotNull();

		assertThat(promotionOrder.isPaid()).isTrue();

		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).saveAndFlush(paymentCaptor.capture());

		Payment savedPayment = paymentCaptor.getValue();

		assertThat(savedPayment.getOrderId()).isEqualTo(ORDER_ID);
		assertThat(savedPayment.getPortOnePaymentId()).isEqualTo(PORT_ONE_PAYMENT_ID);
		assertThat(savedPayment.getAmount()).isEqualTo(ORDER_AMOUNT);
		assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(savedPayment.getPaidAt()).isNotNull();
	}

	@Test
	@DisplayName("주문 row lock 조회 결과가 없으면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPromotionOrderNotFound() {
		// given
		PortOnePaymentInfo portOnePaymentInfo = createPaidPortOnePaymentInfo(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentConfirmTransactionService.confirmPayment(
			MEMBER_ID,
			ORDER_ID,
			portOnePaymentInfo
		))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROMOTION_ORDER_NOT_FOUND);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
	}

	@Test
	@DisplayName("row lock 획득 후 이미 결제 완료된 주문이면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPromotionOrderAlreadyPaid() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		promotionOrder.markAsPaid(LocalDateTime.now());

		PortOnePaymentInfo portOnePaymentInfo = createPaidPortOnePaymentInfo(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));

		// when & then
		assertThatThrownBy(() -> paymentConfirmTransactionService.confirmPayment(
			MEMBER_ID,
			ORDER_ID,
			portOnePaymentInfo
		))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_ALREADY_COMPLETED);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
	}

	@Test
	@DisplayName("이미 결제 완료 Payment가 있으면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPaidPaymentAlreadyExists() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PortOnePaymentInfo portOnePaymentInfo = createPaidPortOnePaymentInfo(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> paymentConfirmTransactionService.confirmPayment(
			MEMBER_ID,
			ORDER_ID,
			portOnePaymentInfo
		))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_ALREADY_COMPLETED);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
	}

	@Test
	@DisplayName("같은 PortOne 결제 ID가 이미 있으면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPortOnePaymentIdDuplicated() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PortOnePaymentInfo portOnePaymentInfo = createPaidPortOnePaymentInfo(ORDER_AMOUNT);
		Payment existingPayment = Payment.createPaidPayment(
			ORDER_ID,
			PORT_ONE_PAYMENT_ID,
			ORDER_AMOUNT,
			null,
			LocalDateTime.now()
		);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.of(existingPayment));

		// when & then
		assertThatThrownBy(() -> paymentConfirmTransactionService.confirmPayment(
			MEMBER_ID,
			ORDER_ID,
			portOnePaymentInfo
		))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATED_PAYMENT);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
	}

	@Test
	@DisplayName("PortOne 결제 금액이 주문 금액과 다르면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPortOneAmountMismatch() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PortOnePaymentInfo portOnePaymentInfo = createPaidPortOnePaymentInfo(9999L);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentConfirmTransactionService.confirmPayment(
			MEMBER_ID,
			ORDER_ID,
			portOnePaymentInfo
		))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
		assertThat(promotionOrder.isWaiting()).isTrue();
	}

	@Test
	@DisplayName("같은 PortOne 결제 ID DB unique 제약에 걸리면 중복 결제로 처리한다")
	void confirmPaymentFailWhenPortOnePaymentIdDuplicatedByDatabaseConstraint() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PortOnePaymentInfo portOnePaymentInfo = createPaidPortOnePaymentInfo(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());
		when(paymentRepository.saveAndFlush(any(Payment.class)))
			.thenThrow(new DataIntegrityViolationException(
				"Duplicate entry for key 'uk_payment_portone_payment_id'"
			));

		// when & then
		assertThatThrownBy(() -> paymentConfirmTransactionService.confirmPayment(
			MEMBER_ID,
			ORDER_ID,
			portOnePaymentInfo
		))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATED_PAYMENT);

		assertThat(promotionOrder.isWaiting()).isTrue();
	}

	@Test
	@DisplayName("PortOne 결제 ID 중복이 아닌 DB 제약 오류는 결제 승인 실패로 처리한다")
	void confirmPaymentFailWhenUnknownDataIntegrityViolationOccurs() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PortOnePaymentInfo portOnePaymentInfo = createPaidPortOnePaymentInfo(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());
		when(paymentRepository.saveAndFlush(any(Payment.class)))
			.thenThrow(new DataIntegrityViolationException("not null constraint violation"));

		// when & then
		assertThatThrownBy(() -> paymentConfirmTransactionService.confirmPayment(
			MEMBER_ID,
			ORDER_ID,
			portOnePaymentInfo
		))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_CONFIRM_FAILED);

		assertThat(promotionOrder.isWaiting()).isTrue();
	}

	private PromotionOrder createWaitingPromotionOrder() {
		PromotionOrder promotionOrder = PromotionOrder.create(
			MEMBER_ID,
			PROMOTION_PRODUCT_ID,
			ORDER_AMOUNT,
			LocalDateTime.now()
		);

		ReflectionTestUtils.setField(promotionOrder, "id", ORDER_ID);

		return promotionOrder;
	}

	private PortOnePaymentInfo createPaidPortOnePaymentInfo(Long amount) {
		return new PortOnePaymentInfo(
			PORT_ONE_PAYMENT_ID,
			"PAID",
			amount
		);
	}
}
