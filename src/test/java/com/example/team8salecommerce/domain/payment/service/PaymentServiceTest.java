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

import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmResponse;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**

 결제 서비스 단위 테스트


 결제 승인 시 주문 조회, 주문 상태 검증, 중복 결제 검증,

 결제 금액 검증, Payment 저장, 주문 상태 변경이 정상적으로 동작하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

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
	private PaymentService paymentService;

	@Test
	@DisplayName("결제 승인에 성공한다")
	void confirmPaymentSuccess() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());
		when(paymentRepository.saveAndFlush(any(Payment.class)))
			.thenAnswer(invocation -> {
				Payment payment = invocation.getArgument(0);

				// 실제 DB 저장 시에는 id가 자동 생성되지만,
				// 단위 테스트에서는 Repository를 Mock으로 사용하므로 직접 id를 넣어준다.
				ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);

				return payment;
			});

		// when
		PaymentConfirmResponse response = paymentService.confirmPayment(MEMBER_ID, request);

		// then
		assertThat(response.orderId()).isEqualTo(ORDER_ID);
		assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
		assertThat(response.portOnePaymentId()).isEqualTo(PORT_ONE_PAYMENT_ID);
		assertThat(response.amount()).isEqualTo(ORDER_AMOUNT);
		assertThat(response.orderStatus()).isEqualTo("PAID");
		assertThat(response.paymentStatus()).isEqualTo("PAID");
		assertThat(response.paidAt()).isNotNull();

		// 주문 상태도 결제 완료 상태로 변경되어야 한다.
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
	@DisplayName("회원 ID가 null이면 결제 승인에 실패한다")
	void confirmPaymentFailWhenMemberIdIsNull() {
		// given
		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(null, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);

		verify(promotionOrderRepository, never()).findByIdAndMemberId(any(), any());
		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));

	}

	@Test
	@DisplayName("특가 주문이 없으면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPromotionOrderNotFound() {
		// given
		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROMOTION_ORDER_NOT_FOUND);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));

	}

	@Test
	@DisplayName("이미 결제 완료된 주문이면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPromotionOrderAlreadyPaid() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		promotionOrder.markAsPaid(LocalDateTime.now());

		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(MEMBER_ID, request))
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
		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(MEMBER_ID, request))
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
		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);
		Payment existingPayment = Payment.createPaidPayment(
			ORDER_ID,
			PORT_ONE_PAYMENT_ID,
			ORDER_AMOUNT,
			null,
			LocalDateTime.now()
		);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.of(existingPayment));

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATED_PAYMENT);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));

	}

	@Test
	@DisplayName("동시에 같은 PortOne 결제 ID가 저장되어 DB unique 제약에 걸리면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPortOnePaymentIdDuplicatedByDatabaseConstraint() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());
		when(paymentRepository.saveAndFlush(any(Payment.class)))
			.thenThrow(new DataIntegrityViolationException("Duplicated portOnePaymentId"));

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATED_PAYMENT);

		// 결제 저장이 실패했으므로 주문 상태도 PAID로 바뀌면 안 된다.
		assertThat(promotionOrder.isWaiting()).isTrue();

	}

	@Test
	@DisplayName("결제 금액이 주문 금액과 다르면 결제 승인에 실패한다")
	void confirmPaymentFailWhenAmountMismatch() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentConfirmRequest request = createRequest(9999L);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));

	}

	/**

	 결제 대기 상태의 특가 주문을 생성한다.


	 PromotionOrder는 실제 DB에 저장되어야 id가 생성되지만,

	 단위 테스트에서는 DB를 사용하지 않으므로 ReflectionTestUtils로 id를 세팅한다.
	 */
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

	/**

	 결제 승인 요청 DTO를 생성한다.
	 */
	private PaymentConfirmRequest createRequest(Long amount) {
		return new PaymentConfirmRequest(
			ORDER_ID,
			PORT_ONE_PAYMENT_ID,
			amount
		);
	}
}
