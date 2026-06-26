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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrderItem;
import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderItemRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionProductRepository;
import com.example.team8salecommerce.domain.refund.dto.RefundResponse;
import com.example.team8salecommerce.domain.refund.entity.Refund;
import com.example.team8salecommerce.domain.refund.entity.RefundReasonType;
import com.example.team8salecommerce.domain.refund.repository.RefundRepository;
import com.example.team8salecommerce.domain.stock.entity.StockHistory;
import com.example.team8salecommerce.domain.stock.repository.StockHistoryRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**
 * RefundCompleteTransactionService 테스트
 *
 * PortOne 환불 성공 이후 내부 상태 변경을 검증한다.
 *
 * 이 단계에서는 아래 작업이 하나의 트랜잭션으로 처리된다.
 * 1. Refund 상태 REFUNDED 변경
 * 2. PromotionOrder 상태 REFUNDED 변경
 * 3. PromotionProduct 이벤트 재고 복구
 * 4. StockHistory 환불 복구 이력 저장
 */
@ExtendWith(MockitoExtension.class)
class RefundCompleteTransactionServiceTest {

	@Mock
	private RefundRepository refundRepository;

	@Mock
	private PromotionOrderRepository promotionOrderRepository;

	@Mock
	private PromotionOrderItemRepository promotionOrderItemRepository;

	@Mock
	private PromotionProductRepository promotionProductRepository;

	@Mock
	private StockHistoryRepository stockHistoryRepository;

	@InjectMocks
	private RefundCompleteTransactionService refundCompleteTransactionService;

	@Test
	@DisplayName("환불 완료 처리에 성공하면 환불 완료, 주문 환불 완료, 재고 복구, 재고 이력이 저장된다")
	void completeRefundSuccess() {
		// given
		Long refundId = 1L;
		Long orderId = 10L;
		Long paymentId = 20L;
		Long memberId = 1L;
		Long promotionProductId = 100L;
		Long productId = 200L;
		Long refundAmount = 7000L;

		RefundProcessingContext context = createContext(
			refundId,
			orderId,
			paymentId,
			memberId,
			refundAmount
		);

		Refund refund = createPortOneRefundSucceededRefund(
			refundId,
			orderId,
			paymentId,
			memberId,
			refundAmount
		);

		PromotionOrder promotionOrder = createRefundRequestedPromotionOrder(
			orderId,
			memberId,
			promotionProductId,
			refundAmount
		);

		PromotionOrderItem orderItem = createPromotionOrderItem(
			orderId,
			promotionProductId,
			productId,
			1,
			refundAmount
		);

		PromotionProduct promotionProduct = createPromotionProduct(
			promotionProductId,
			productId,
			10
		);

		// 선착순 구매로 1개가 차감된 상태를 만든다.
		// 환불 완료 시 9개에서 10개로 복구되는지 검증한다.
		promotionProduct.decreaseStock(1);

		when(refundRepository.findByIdForUpdate(refundId))
			.thenReturn(Optional.of(refund));

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		when(promotionOrderItemRepository.findByPromotionOrderId(orderId))
			.thenReturn(Optional.of(orderItem));

		when(promotionProductRepository.findByIdForUpdate(promotionProductId))
			.thenReturn(Optional.of(promotionProduct));

		// when
		RefundResponse response = refundCompleteTransactionService.completeRefund(context);

		// then
		assertEquals(refundId, response.refundId());
		assertEquals(orderId, response.orderId());
		assertEquals(paymentId, response.paymentId());
		assertEquals(refundAmount, response.refundAmount());
		assertEquals("REFUNDED", response.refundStatus());
		assertEquals(1, response.restoredEventStock());
		assertEquals(10, response.remainingEventStock());
		assertNotNull(response.completedAt());

		assertTrue(refund.isRefunded());
		assertTrue(promotionOrder.isRefunded());
		assertEquals(10, promotionProduct.getRemainingEventStock());

		ArgumentCaptor<StockHistory> stockHistoryCaptor =
			ArgumentCaptor.forClass(StockHistory.class);

		verify(stockHistoryRepository).save(stockHistoryCaptor.capture());

		StockHistory savedStockHistory = stockHistoryCaptor.getValue();

		assertEquals(productId, savedStockHistory.getProductId());
		assertEquals(promotionProductId, savedStockHistory.getPromotionProductId());
		assertEquals(orderId, savedStockHistory.getOrderId());
		assertEquals(paymentId, savedStockHistory.getPaymentId());
		assertEquals(refundId, savedStockHistory.getRefundId());
		assertEquals(1, savedStockHistory.getQuantity());
		assertEquals(9, savedStockHistory.getStockBefore());
		assertEquals(10, savedStockHistory.getStockAfter());
		assertEquals("RESTORE", savedStockHistory.getType().name());
		assertEquals("REFUND_COMPLETED", savedStockHistory.getReason().name());
	}

	@Test
	@DisplayName("환불 처리 context가 없으면 환불 완료 처리에 실패한다")
	void completeRefundFailWhenContextIsNull() {
		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundCompleteTransactionService.completeRefund(null)
		);

		// then
		assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

		verify(refundRepository, never()).findByIdForUpdate(any());
		verify(stockHistoryRepository, never()).save(any());
	}

	@Test
	@DisplayName("환불 정보가 없으면 환불 완료 처리에 실패한다")
	void completeRefundFailWhenRefundNotFound() {
		// given
		RefundProcessingContext context = createContext(
			1L,
			10L,
			20L,
			1L,
			7000L
		);

		when(refundRepository.findByIdForUpdate(1L))
			.thenReturn(Optional.empty());

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundCompleteTransactionService.completeRefund(context)
		);

		// then
		assertEquals(ErrorCode.REFUND_NOT_FOUND, exception.getErrorCode());

		verify(stockHistoryRepository, never()).save(any());
	}

	@Test
	@DisplayName("PortOne 환불 성공 상태가 아니면 환불 완료 처리에 실패한다")
	void completeRefundFailWhenRefundIsNotPortOneRefundSucceeded() {
		// given
		Long refundId = 1L;

		RefundProcessingContext context = createContext(
			refundId,
			10L,
			20L,
			1L,
			7000L
		);

		Refund refund = createRequestedRefund(
			refundId,
			10L,
			20L,
			1L,
			7000L
		);

		when(refundRepository.findByIdForUpdate(refundId))
			.thenReturn(Optional.of(refund));

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundCompleteTransactionService.completeRefund(context)
		);

		// then
		assertEquals(ErrorCode.REFUND_NOT_ALLOWED, exception.getErrorCode());

		verify(stockHistoryRepository, never()).save(any());
	}

	@Test
	@DisplayName("환불 대상 주문이 없으면 환불 완료 처리에 실패한다")
	void completeRefundFailWhenPromotionOrderNotFound() {
		// given
		Long refundId = 1L;
		Long orderId = 10L;
		Long memberId = 1L;

		RefundProcessingContext context = createContext(
			refundId,
			orderId,
			20L,
			memberId,
			7000L
		);

		Refund refund = createPortOneRefundSucceededRefund(
			refundId,
			orderId,
			20L,
			memberId,
			7000L
		);

		when(refundRepository.findByIdForUpdate(refundId))
			.thenReturn(Optional.of(refund));

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.empty());

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundCompleteTransactionService.completeRefund(context)
		);

		// then
		assertEquals(ErrorCode.PROMOTION_ORDER_NOT_FOUND, exception.getErrorCode());

		verify(stockHistoryRepository, never()).save(any());
	}

	@Test
	@DisplayName("환불 대상 주문 상품이 없으면 환불 완료 처리에 실패한다")
	void completeRefundFailWhenPromotionOrderItemNotFound() {
		// given
		Long refundId = 1L;
		Long orderId = 10L;
		Long memberId = 1L;

		RefundProcessingContext context = createContext(
			refundId,
			orderId,
			20L,
			memberId,
			7000L
		);

		Refund refund = createPortOneRefundSucceededRefund(
			refundId,
			orderId,
			20L,
			memberId,
			7000L
		);

		PromotionOrder promotionOrder = createRefundRequestedPromotionOrder(
			orderId,
			memberId,
			100L,
			7000L
		);

		when(refundRepository.findByIdForUpdate(refundId))
			.thenReturn(Optional.of(refund));

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		when(promotionOrderItemRepository.findByPromotionOrderId(orderId))
			.thenReturn(Optional.empty());

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundCompleteTransactionService.completeRefund(context)
		);

		// then
		assertEquals(ErrorCode.PROMOTION_ORDER_ITEM_NOT_FOUND, exception.getErrorCode());

		verify(stockHistoryRepository, never()).save(any());
	}

	@Test
	@DisplayName("특가 상품이 없으면 환불 완료 처리에 실패한다")
	void completeRefundFailWhenPromotionProductNotFound() {
		// given
		Long refundId = 1L;
		Long orderId = 10L;
		Long memberId = 1L;
		Long promotionProductId = 100L;
		Long productId = 200L;

		RefundProcessingContext context = createContext(
			refundId,
			orderId,
			20L,
			memberId,
			7000L
		);

		Refund refund = createPortOneRefundSucceededRefund(
			refundId,
			orderId,
			20L,
			memberId,
			7000L
		);

		PromotionOrder promotionOrder = createRefundRequestedPromotionOrder(
			orderId,
			memberId,
			promotionProductId,
			7000L
		);

		PromotionOrderItem orderItem = createPromotionOrderItem(
			orderId,
			promotionProductId,
			productId,
			1,
			7000L
		);

		when(refundRepository.findByIdForUpdate(refundId))
			.thenReturn(Optional.of(refund));

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(orderId, memberId))
			.thenReturn(Optional.of(promotionOrder));

		when(promotionOrderItemRepository.findByPromotionOrderId(orderId))
			.thenReturn(Optional.of(orderItem));

		when(promotionProductRepository.findByIdForUpdate(promotionProductId))
			.thenReturn(Optional.empty());

		// when
		CustomException exception = assertThrows(
			CustomException.class,
			() -> refundCompleteTransactionService.completeRefund(context)
		);

		// then
		assertEquals(ErrorCode.PROMOTION_PRODUCT_NOT_FOUND, exception.getErrorCode());

		verify(stockHistoryRepository, never()).save(any());
	}

	/**
	 * 테스트용 환불 처리 context를 생성한다.
	 */
	private RefundProcessingContext createContext(
		Long refundId,
		Long orderId,
		Long paymentId,
		Long memberId,
		Long refundAmount
	) {
		return new RefundProcessingContext(
			refundId,
			orderId,
			paymentId,
			memberId,
			"payment-123",
			refundAmount,
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
		Long memberId,
		Long refundAmount
	) {
		Refund refund = Refund.createRequest(
			orderId,
			paymentId,
			memberId,
			RefundReasonType.CHANGE_OF_MIND,
			"단순 변심",
			refundAmount,
			LocalDateTime.now()
		);

		ReflectionTestUtils.setField(refund, "id", refundId);

		return refund;
	}

	/**
	 * 테스트용 PortOne 환불 성공 상태 환불 엔티티를 생성한다.
	 *
	 * 환불 완료 처리 Service는 PortOne 환불 성공 상태인 Refund만 완료 처리할 수 있다.
	 */
	private Refund createPortOneRefundSucceededRefund(
		Long refundId,
		Long orderId,
		Long paymentId,
		Long memberId,
		Long refundAmount
	) {
		Refund refund = createRequestedRefund(
			refundId,
			orderId,
			paymentId,
			memberId,
			refundAmount
		);

		refund.recordPortOneRefundSuccess(
			"cancel-123",
			"SUCCEEDED",
			LocalDateTime.now()
		);

		return refund;
	}

	/**
	 * 테스트용 환불 요청 상태 특가 주문을 생성한다.
	 */
	private PromotionOrder createRefundRequestedPromotionOrder(
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
		promotionOrder.requestRefund(LocalDateTime.now());

		return promotionOrder;
	}

	/**
	 * 테스트용 특가 주문 상품을 생성한다.
	 */
	private PromotionOrderItem createPromotionOrderItem(
		Long orderId,
		Long promotionProductId,
		Long productId,
		Integer quantity,
		Long unitPrice
	) {
		PromotionOrderItem orderItem = PromotionOrderItem.create(
			orderId,
			promotionProductId,
			productId,
			"테스트 상품",
			quantity,
			unitPrice
		);

		ReflectionTestUtils.setField(orderItem, "id", 1000L);

		return orderItem;
	}

	/**
	 * 테스트용 특가 상품을 생성한다.
	 */
	private PromotionProduct createPromotionProduct(
		Long promotionProductId,
		Long productId,
		Integer totalEventStock
	) {
		PromotionProduct promotionProduct = PromotionProduct.create(
			productId,
			"테스트 특가 상품",
			7000L,
			30,
			totalEventStock,
			LocalDateTime.now().minusMinutes(10),
			LocalDateTime.now().plusHours(1)
		);

		ReflectionTestUtils.setField(promotionProduct, "id", promotionProductId);

		return promotionProduct;
	}
}
