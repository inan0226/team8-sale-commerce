package com.example.team8salecommerce.domain.refund.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.refund.dto.RefundRequest;
import com.example.team8salecommerce.domain.refund.entity.Refund;
import com.example.team8salecommerce.domain.refund.entity.RefundReasonType;
import com.example.team8salecommerce.domain.refund.entity.RefundStatus;
import com.example.team8salecommerce.domain.refund.repository.RefundRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**
 * RefundRequestTransactionService 테스트
 *
 * 환불 요청 생성 단계의 검증을 담당한다.
 *
 * 이 단계에서는 외부 PortOne API를 호출하지 않고,
 * 내부 DB 상태만 REFUND_REQUEST로 변경한다.
 */
@ExtendWith(MockitoExtension.class)
class RefundRequestTransactionServiceTest {

	@Mock
	private RefundRepository refundRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PromotionOrderRepository promotionOrderRepository;

	@InjectMocks
	private RefundRequestTransactionService refundRequestTransactionService;

	@Test
	@DisplayName("결제 완료 주문이면 환불 요청 생성에 성공한다")
	void requestRefundSuccess() {
		// given
		Long memberId = 1L;
		Long orderId = 10L;
		Long promotionProductId = 100L;
		Long paymentId = 20L;
		Long refundId = 30L;
		Long amount = 7000L;

		RefundRequest request = new RefundRequest(
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);

		PromotionOrder promotionOrder = createPaidPromotionOrder(
			orderId,
			memberId,
			promotionProductId,
			amount
		);

		Payment payment = createPaidPayment(
			paymentId,
			orderId,
			"payment-123",
			amount
		);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		when(refundRepository.existsByOrderIdAndStatus(orderId, RefundStatus.REFUND_REQUEST))
			.thenReturn(false);

		when(refundRepository.existsByOrderIdAndStatus(orderId, RefundStatus.REFUNDED))
			.thenReturn(false);

		when(paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PAID))
			.thenReturn(Optional.of(payment));

		when(refundRepository.saveAndFlush(any(Refund.class)))
			.thenAnswer(invocation -> {
				Refund refund = invocation.getArgument(0);
				ReflectionTestUtils.setField(refund, "id", refundId);
				return refund;
			});

		// when
		RefundProcessingContext context = refundRequestTransactionService.requestRefund(
			memberId,
			orderId,
			request
		);

		// then
		assertEquals(refundId, context.refundId());
		assertEquals(orderId, context.orderId());
		assertEquals(paymentId, context.paymentId());
		assertEquals(memberId, context.memberId());
		assertEquals("payment-123", context.portOnePaymentId());
		assertEquals(amount, context.refundAmount());
		assertEquals(RefundReasonType.CHANGE_OF_MIND, context.reasonType());
		assertEquals("단순 변심", context.reasonDetail());

		assertTrue(promotionOrder.isRefundRequested());

		ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);

		verify(refundRepository).saveAndFlush(refundCaptor.capture());

		Refund savedRefund = refundCaptor.getValue();

		assertEquals(orderId, savedRefund.getOrderId());
		assertEquals(paymentId, savedRefund.getPaymentId());
		assertEquals(memberId, savedRefund.getMemberId());
		assertEquals(amount, savedRefund.getRefundAmount());
		assertEquals(RefundStatus.REFUND_REQUEST, savedRefund.getStatus());
		assertEquals(RefundReasonType.CHANGE_OF_MIND, savedRefund.getRefundReasonType());
		assertEquals("단순 변심", savedRefund.getReasonDetail());
	}

	@Test
	@DisplayName("인증 회원 ID가 없으면 환불 요청에 실패한다")
	void requestRefundFailWhenMemberIdIsNull() {
		// given
		RefundRequest request = new RefundRequest(
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundRequestTransactionService.requestRefund(null, 10L, request)
		);

		// then
		assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());

		verify(promotionOrderRepository, never()).findByIdAndMemberIdForUpdate(any(), any());
		verify(refundRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("주문 ID가 0 이하이면 환불 요청에 실패한다")
	void requestRefundFailWhenOrderIdIsInvalid() {
		// given
		RefundRequest request = new RefundRequest(
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundRequestTransactionService.requestRefund(1L, 0L, request)
		);

		// then
		assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

		verify(promotionOrderRepository, never()).findByIdAndMemberIdForUpdate(any(), any());
		verify(refundRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("본인 특가 주문이 없으면 환불 요청에 실패한다")
	void requestRefundFailWhenPromotionOrderNotFound() {
		// given
		Long memberId = 1L;
		Long orderId = 10L;

		RefundRequest request = new RefundRequest(
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.empty());

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundRequestTransactionService.requestRefund(memberId, orderId, request)
		);

		// then
		assertEquals(ErrorCode.PROMOTION_ORDER_NOT_FOUND, exception.getErrorCode());

		verify(refundRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("결제 완료 상태가 아닌 주문이면 환불 요청에 실패한다")
	void requestRefundFailWhenPromotionOrderIsNotPaid() {
		// given
		Long memberId = 1L;
		Long orderId = 10L;

		RefundRequest request = new RefundRequest(
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);

		PromotionOrder promotionOrder = PromotionOrder.create(
			memberId,
			100L,
			7000L,
			LocalDateTime.now()
		);

		ReflectionTestUtils.setField(promotionOrder, "id", orderId);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundRequestTransactionService.requestRefund(memberId, orderId, request)
		);

		// then
		assertEquals(ErrorCode.REFUND_NOT_ALLOWED, exception.getErrorCode());

		verify(refundRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("이미 환불 요청된 주문이면 환불 요청에 실패한다")
	void requestRefundFailWhenAlreadyRefundRequested() {
		// given
		Long memberId = 1L;
		Long orderId = 10L;

		RefundRequest request = new RefundRequest(
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);

		PromotionOrder promotionOrder = createPaidPromotionOrder(
			orderId,
			memberId,
			100L,
			7000L
		);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		when(refundRepository.existsByOrderIdAndStatus(orderId, RefundStatus.REFUND_REQUEST))
			.thenReturn(true);

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundRequestTransactionService.requestRefund(memberId, orderId, request)
		);

		// then
		assertEquals(ErrorCode.REFUND_ALREADY_REQUESTED, exception.getErrorCode());

		verify(refundRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("이미 환불 완료된 주문이면 환불 요청에 실패한다")
	void requestRefundFailWhenAlreadyRefunded() {
		// given
		Long memberId = 1L;
		Long orderId = 10L;

		RefundRequest request = new RefundRequest(
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);

		PromotionOrder promotionOrder = createPaidPromotionOrder(
			orderId,
			memberId,
			100L,
			7000L
		);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		when(refundRepository.existsByOrderIdAndStatus(orderId, RefundStatus.REFUND_REQUEST))
			.thenReturn(false);

		when(refundRepository.existsByOrderIdAndStatus(orderId, RefundStatus.REFUNDED))
			.thenReturn(true);

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundRequestTransactionService.requestRefund(memberId, orderId, request)
		);

		// then
		assertEquals(ErrorCode.ALREADY_REFUNDED, exception.getErrorCode());

		verify(refundRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("결제 완료 정보가 없으면 환불 요청에 실패한다")
	void requestRefundFailWhenPaidPaymentNotFound() {
		// given
		Long memberId = 1L;
		Long orderId = 10L;

		RefundRequest request = new RefundRequest(
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);

		PromotionOrder promotionOrder = createPaidPromotionOrder(
			orderId,
			memberId,
			100L,
			7000L
		);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		when(refundRepository.existsByOrderIdAndStatus(orderId, RefundStatus.REFUND_REQUEST))
			.thenReturn(false);

		when(refundRepository.existsByOrderIdAndStatus(orderId, RefundStatus.REFUNDED))
			.thenReturn(false);

		when(paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PAID))
			.thenReturn(Optional.empty());

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundRequestTransactionService.requestRefund(memberId, orderId, request)
		);

		// then
		assertEquals(ErrorCode.PAYMENT_NOT_FOUND, exception.getErrorCode());

		verify(refundRepository, never()).saveAndFlush(any());
	}

	/**
	 * 테스트용 결제 완료 특가 주문을 생성한다.
	 */
	private PromotionOrder createPaidPromotionOrder(
		Long orderId,
		Long memberId,
		Long promotionProductId,
		Long totalAmount
	) {
		PromotionOrder promotionOrder = PromotionOrder.create(
			memberId,
			promotionProductId,
			totalAmount,
			LocalDateTime.now()
		);

		ReflectionTestUtils.setField(promotionOrder, "id", orderId);
		promotionOrder.markAsPaid(LocalDateTime.now());

		return promotionOrder;
	}

	/**
	 * 테스트용 결제 완료 Payment를 생성한다.
	 */
	private Payment createPaidPayment(
		Long paymentId,
		Long orderId,
		String portOnePaymentId,
		Long amount
	) {
		Payment payment = Payment.createPaidPayment(
			orderId,
			portOnePaymentId,
			amount,
			"CARD",
			LocalDateTime.now()
		);

		ReflectionTestUtils.setField(payment, "id", paymentId);

		return payment;
	}
}
