package com.example.team8salecommerce.domain.refund.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.refund.entity.Refund;
import com.example.team8salecommerce.domain.refund.entity.RefundReasonType;
import com.example.team8salecommerce.domain.refund.entity.RefundStatus;
import com.example.team8salecommerce.domain.refund.repository.RefundRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**
 * RefundFailTransactionService 테스트
 *
 * PortOne 환불 요청 실패 이후 내부 상태 변경을 검증한다.
 *
 * 이 단계에서는 아래 작업이 하나의 트랜잭션으로 처리된다.
 * 1. Refund 상태 REFUND_FAILED 변경
 * 2. Refund 실패 시간/실패 사유 저장
 * 3. PromotionOrder 상태 PAID 복구
 */
@ExtendWith(MockitoExtension.class)
class RefundFailTransactionServiceTest {

	@Mock
	private RefundRepository refundRepository;

	@Mock
	private PromotionOrderRepository promotionOrderRepository;

	@InjectMocks
	private RefundFailTransactionService refundFailTransactionService;

	@Test
	@DisplayName("환불 실패 처리에 성공하면 환불은 실패 상태가 되고 주문은 결제 완료 상태로 복구된다")
	void failRefundSuccess() {
		// given
		Long refundId = 1L;
		Long orderId = 10L;
		Long paymentId = 20L;
		Long memberId = 1L;

		RefundProcessingContext context = createContext(
			refundId,
			orderId,
			paymentId,
			memberId
		);

		Refund refund = createRequestedRefund(
			refundId,
			orderId,
			paymentId,
			memberId
		);

		PromotionOrder promotionOrder = createRefundRequestedPromotionOrder(
			orderId,
			memberId
		);

		when(refundRepository.findByIdForUpdate(refundId))
			.thenReturn(Optional.of(refund));

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		// when
		refundFailTransactionService.failRefund(
			context,
			"PortOne 환불 요청에 실패했습니다."
		);

		// then
		assertEquals(RefundStatus.REFUND_FAILED, refund.getStatus());
		assertNotNull(refund.getFailedAt());
		assertEquals("PortOne 환불 요청에 실패했습니다.", refund.getFailureReason());

		assertTrue(promotionOrder.isPaid());
	}

	@Test
	@DisplayName("환불 실패 사유가 비어 있으면 기본 실패 사유를 저장한다")
	void failRefundSuccessWhenFailureReasonIsBlank() {
		// given
		Long refundId = 1L;
		Long orderId = 10L;
		Long paymentId = 20L;
		Long memberId = 1L;

		RefundProcessingContext context = createContext(
			refundId,
			orderId,
			paymentId,
			memberId
		);

		Refund refund = createRequestedRefund(
			refundId,
			orderId,
			paymentId,
			memberId
		);

		PromotionOrder promotionOrder = createRefundRequestedPromotionOrder(
			orderId,
			memberId
		);

		when(refundRepository.findByIdForUpdate(refundId))
			.thenReturn(Optional.of(refund));

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		// when
		refundFailTransactionService.failRefund(
			context,
			" "
		);

		// then
		assertEquals(RefundStatus.REFUND_FAILED, refund.getStatus());
		assertEquals("PortOne 환불 요청에 실패했습니다.", refund.getFailureReason());
		assertTrue(promotionOrder.isPaid());
	}

	@Test
	@DisplayName("환불 실패 사유가 255자를 초과하면 255자로 잘라서 저장한다")
	void failRefundSuccessWhenFailureReasonIsTooLong() {
		// given
		Long refundId = 1L;
		Long orderId = 10L;
		Long paymentId = 20L;
		Long memberId = 1L;

		RefundProcessingContext context = createContext(
			refundId,
			orderId,
			paymentId,
			memberId
		);

		Refund refund = createRequestedRefund(
			refundId,
			orderId,
			paymentId,
			memberId
		);

		PromotionOrder promotionOrder = createRefundRequestedPromotionOrder(
			orderId,
			memberId
		);

		String longFailureReason = "a".repeat(300);

		when(refundRepository.findByIdForUpdate(refundId))
			.thenReturn(Optional.of(refund));

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		// when
		refundFailTransactionService.failRefund(
			context,
			longFailureReason
		);

		// then
		assertEquals(RefundStatus.REFUND_FAILED, refund.getStatus());
		assertEquals(255, refund.getFailureReason().length());
		assertTrue(promotionOrder.isPaid());
	}

	@Test
	@DisplayName("환불 처리 context가 없으면 환불 실패 처리에 실패한다")
	void failRefundFailWhenContextIsNull() {
		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundFailTransactionService.failRefund(null, "실패")
		);

		// then
		assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

		verify(refundRepository, never()).findByIdForUpdate(any());
		verify(promotionOrderRepository, never()).findByIdAndMemberIdForUpdate(any(), any());
	}

	@Test
	@DisplayName("환불 정보가 없으면 환불 실패 처리에 실패한다")
	void failRefundFailWhenRefundNotFound() {
		// given
		RefundProcessingContext context = createContext(
			1L,
			10L,
			20L,
			1L
		);

		when(refundRepository.findByIdForUpdate(1L))
			.thenReturn(Optional.empty());

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundFailTransactionService.failRefund(context, "실패")
		);

		// then
		assertEquals(ErrorCode.REFUND_NOT_FOUND, exception.getErrorCode());

		verify(promotionOrderRepository, never()).findByIdAndMemberIdForUpdate(any(), any());
	}

	@Test
	@DisplayName("환불 요청 상태가 아니면 환불 실패 처리에 실패한다")
	void failRefundFailWhenRefundIsNotRequested() {
		// given
		Long refundId = 1L;

		RefundProcessingContext context = createContext(
			refundId,
			10L,
			20L,
			1L
		);

		Refund refund = createRequestedRefund(
			refundId,
			10L,
			20L,
			1L
		);

		refund.complete(LocalDateTime.now());

		when(refundRepository.findByIdForUpdate(refundId))
			.thenReturn(Optional.of(refund));

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundFailTransactionService.failRefund(context, "실패")
		);

		// then
		assertEquals(ErrorCode.REFUND_NOT_ALLOWED, exception.getErrorCode());

		verify(promotionOrderRepository, never()).findByIdAndMemberIdForUpdate(any(), any());
	}

	@Test
	@DisplayName("환불 대상 주문이 없으면 환불 실패 처리에 실패한다")
	void failRefundFailWhenPromotionOrderNotFound() {
		// given
		Long refundId = 1L;
		Long orderId = 10L;
		Long memberId = 1L;

		RefundProcessingContext context = createContext(
			refundId,
			orderId,
			20L,
			memberId
		);

		Refund refund = createRequestedRefund(
			refundId,
			orderId,
			20L,
			memberId
		);

		when(refundRepository.findByIdForUpdate(refundId))
			.thenReturn(Optional.of(refund));

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.empty());

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundFailTransactionService.failRefund(context, "실패")
		);

		// then
		assertEquals(ErrorCode.PROMOTION_ORDER_NOT_FOUND, exception.getErrorCode());
	}

	/**
	 * 테스트용 환불 처리 context를 생성한다.
	 */
	private RefundProcessingContext createContext(
		Long refundId,
		Long orderId,
		Long paymentId,
		Long memberId
	) {
		return new RefundProcessingContext(
			refundId,
			orderId,
			paymentId,
			memberId,
			"payment-123",
			7000L,
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심"
		);
	}

	/**
	 * 테스트용 환불 요청 엔티티를 생성한다.
	 */
	private Refund createRequestedRefund(
		Long refundId,
		Long orderId,
		Long paymentId,
		Long memberId
	) {
		Refund refund = Refund.createRequest(
			orderId,
			paymentId,
			memberId,
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심",
			7000L,
			LocalDateTime.now()
		);

		ReflectionTestUtils.setField(refund, "id", refundId);

		return refund;
	}

	/**
	 * 테스트용 환불 요청 상태 특가 주문을 생성한다.
	 */
	private PromotionOrder createRefundRequestedPromotionOrder(
		Long orderId,
		Long memberId
	) {
		PromotionOrder promotionOrder = PromotionOrder.create(
			memberId,
			100L,
			7000L,
			LocalDateTime.now()
		);

		ReflectionTestUtils.setField(promotionOrder, "id", orderId);

		promotionOrder.markAsPaid(LocalDateTime.now());
		promotionOrder.requestRefund(LocalDateTime.now());

		return promotionOrder;
	}
}
