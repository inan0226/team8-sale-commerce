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

import com.example.team8salecommerce.domain.payment.dto.PaymentFailRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentFailResponse;
import com.example.team8salecommerce.domain.payment.entity.Payment;
import com.example.team8salecommerce.domain.payment.entity.PaymentStatus;
import com.example.team8salecommerce.domain.payment.repository.PaymentRepository;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrderItem;
import com.example.team8salecommerce.domain.promotion.entity.PromotionProduct;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderItemRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.domain.promotion.repository.PromotionProductRepository;
import com.example.team8salecommerce.domain.stock.entity.StockChangeReason;
import com.example.team8salecommerce.domain.stock.entity.StockChangeType;
import com.example.team8salecommerce.domain.stock.entity.StockHistory;
import com.example.team8salecommerce.domain.stock.repository.StockHistoryRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**
 * 결제 실패 처리 Service 단위 테스트
 *
 * 결제 실패 처리 시 실패 Payment 저장, 주문 상태 변경,
 * 이벤트 재고 복구, 재고 이력 저장이 정상적으로 동작하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentFailServiceTest {

	private static final Long MEMBER_ID = 1L;
	private static final Long ORDER_ID = 10L;
	private static final Long PROMOTION_PRODUCT_ID = 100L;
	private static final Long PRODUCT_ID = 1000L;
	private static final Long PAYMENT_ID = 500L;
	private static final String PORT_ONE_PAYMENT_ID = "payment-fail-123";
	private static final Long ORDER_AMOUNT = 14000L;
	private static final Integer PURCHASE_QUANTITY = 2;
	private static final Long UNIT_PRICE = 7000L;
	private static final String FAILURE_REASON = "카드 한도 초과";

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PromotionOrderRepository promotionOrderRepository;

	@Mock
	private PromotionOrderItemRepository promotionOrderItemRepository;

	@Mock
	private PromotionProductRepository promotionProductRepository;

	@Mock
	private StockHistoryRepository stockHistoryRepository;

	@InjectMocks
	private PaymentFailService paymentFailService;

	@Test
	@DisplayName("결제 실패 처리에 성공한다")
	void failPaymentSuccess() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PromotionOrderItem promotionOrderItem = createPromotionOrderItem();
		PromotionProduct promotionProduct = createPromotionProduct();
		PaymentFailRequest request = createRequest(ORDER_AMOUNT);

		// 선착순 구매로 이미 2개가 차감된 상태를 만든다.
		promotionProduct.decreaseStock(PURCHASE_QUANTITY);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.FAILED))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());
		when(promotionOrderItemRepository.findByPromotionOrderId(ORDER_ID))
			.thenReturn(Optional.of(promotionOrderItem));
		when(promotionProductRepository.findByIdForUpdate(PROMOTION_PRODUCT_ID))
			.thenReturn(Optional.of(promotionProduct));
		when(paymentRepository.saveAndFlush(any(Payment.class)))
			.thenAnswer(invocation -> {
				Payment payment = invocation.getArgument(0);

				// 실제 DB 저장 시에는 id가 자동 생성되지만,
				// 단위 테스트에서는 Repository를 Mock으로 사용하므로 직접 id를 세팅한다.
				ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);

				return payment;
			});

		// when
		PaymentFailResponse response = paymentFailService.failPayment(MEMBER_ID, request);

		// then
		assertThat(response.orderId()).isEqualTo(ORDER_ID);
		assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
		assertThat(response.portOnePaymentId()).isEqualTo(PORT_ONE_PAYMENT_ID);
		assertThat(response.amount()).isEqualTo(ORDER_AMOUNT);
		assertThat(response.orderStatus()).isEqualTo("PAYMENT_FAILED");
		assertThat(response.paymentStatus()).isEqualTo("FAILED");
		assertThat(response.failedAt()).isNotNull();
		assertThat(response.failureReason()).isEqualTo(FAILURE_REASON);

		// 주문 상태가 결제 실패 상태로 변경되어야 한다.
		assertThat(promotionOrder.isWaiting()).isFalse();

		// 차감됐던 이벤트 재고가 다시 복구되어야 한다.
		assertThat(promotionProduct.getRemainingEventStock()).isEqualTo(5);

		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).saveAndFlush(paymentCaptor.capture());

		Payment savedPayment = paymentCaptor.getValue();

		assertThat(savedPayment.getOrderId()).isEqualTo(ORDER_ID);
		assertThat(savedPayment.getPortOnePaymentId()).isEqualTo(PORT_ONE_PAYMENT_ID);
		assertThat(savedPayment.getAmount()).isEqualTo(ORDER_AMOUNT);
		assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
		assertThat(savedPayment.getFailedAt()).isNotNull();
		assertThat(savedPayment.getFailureReason()).isEqualTo(FAILURE_REASON);

		ArgumentCaptor<StockHistory> stockHistoryCaptor = ArgumentCaptor.forClass(StockHistory.class);
		verify(stockHistoryRepository).save(stockHistoryCaptor.capture());

		StockHistory stockHistory = stockHistoryCaptor.getValue();

		assertThat(stockHistory.getProductId()).isEqualTo(PRODUCT_ID);
		assertThat(stockHistory.getPromotionProductId()).isEqualTo(PROMOTION_PRODUCT_ID);
		assertThat(stockHistory.getOrderId()).isEqualTo(ORDER_ID);
		assertThat(stockHistory.getPaymentId()).isEqualTo(PAYMENT_ID);
		assertThat(stockHistory.getType()).isEqualTo(StockChangeType.RESTORE);
		assertThat(stockHistory.getReason()).isEqualTo(StockChangeReason.PAYMENT_FAILED);
		assertThat(stockHistory.getQuantity()).isEqualTo(PURCHASE_QUANTITY);
		assertThat(stockHistory.getStockBefore()).isEqualTo(3);
		assertThat(stockHistory.getStockAfter()).isEqualTo(5);
	}

	@Test
	@DisplayName("회원 ID가 null이면 결제 실패 처리에 실패한다")
	void failPaymentFailWhenMemberIdIsNull() {
		// given
		PaymentFailRequest request = createRequest(ORDER_AMOUNT);

		// when & then
		assertThatThrownBy(() -> paymentFailService.failPayment(null, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);

		verify(promotionOrderRepository, never()).findByIdAndMemberIdForUpdate(any(), any());
		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
		verify(stockHistoryRepository, never()).save(any(StockHistory.class));
	}

	@Test
	@DisplayName("특가 주문이 없으면 결제 실패 처리에 실패한다")
	void failPaymentFailWhenPromotionOrderNotFound() {
		// given
		PaymentFailRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentFailService.failPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROMOTION_ORDER_NOT_FOUND);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
		verify(stockHistoryRepository, never()).save(any(StockHistory.class));
	}

	@Test
	@DisplayName("주문이 결제 대기 상태가 아니면 결제 실패 처리에 실패한다")
	void failPaymentFailWhenPromotionOrderIsNotWaiting() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		promotionOrder.markAsPaid(LocalDateTime.now());

		PaymentFailRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));

		// when & then
		assertThatThrownBy(() -> paymentFailService.failPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_PROMOTION_ORDER_STATUS);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
		verify(stockHistoryRepository, never()).save(any(StockHistory.class));
	}

	@Test
	@DisplayName("이미 결제 완료 Payment가 있으면 결제 실패 처리에 실패한다")
	void failPaymentFailWhenPaidPaymentAlreadyExists() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentFailRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> paymentFailService.failPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_ALREADY_COMPLETED);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
		verify(stockHistoryRepository, never()).save(any(StockHistory.class));
	}

	@Test
	@DisplayName("이미 결제 실패 Payment가 있으면 결제 실패 처리에 실패한다")
	void failPaymentFailWhenFailedPaymentAlreadyExists() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentFailRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.FAILED))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> paymentFailService.failPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_ALREADY_FAILED);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
		verify(stockHistoryRepository, never()).save(any(StockHistory.class));
	}

	@Test
	@DisplayName("같은 PortOne 결제 ID가 이미 있으면 결제 실패 처리에 실패한다")
	void failPaymentFailWhenPortOnePaymentIdDuplicated() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		Payment existingPayment = Payment.createFailedPayment(
			ORDER_ID,
			PORT_ONE_PAYMENT_ID,
			ORDER_AMOUNT,
			null,
			LocalDateTime.now(),
			FAILURE_REASON
		);
		PaymentFailRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.FAILED))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.of(existingPayment));

		// when & then
		assertThatThrownBy(() -> paymentFailService.failPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATED_PAYMENT);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
		verify(stockHistoryRepository, never()).save(any(StockHistory.class));
	}

	@Test
	@DisplayName("결제 실패 금액이 주문 금액과 다르면 결제 실패 처리에 실패한다")
	void failPaymentFailWhenAmountMismatch() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentFailRequest request = createRequest(9999L);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.FAILED))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> paymentFailService.failPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

		verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
		verify(stockHistoryRepository, never()).save(any(StockHistory.class));
	}

	@Test
	@DisplayName("PortOne 결제 ID unique 제약 위반이면 중복 결제 예외가 발생한다")
	void failPaymentFailWhenPortOnePaymentIdUniqueConstraintViolation() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PromotionOrderItem promotionOrderItem = createPromotionOrderItem();
		PromotionProduct promotionProduct = createPromotionProduct();
		PaymentFailRequest request = createRequest(ORDER_AMOUNT);

		promotionProduct.decreaseStock(PURCHASE_QUANTITY);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.FAILED))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());
		when(promotionOrderItemRepository.findByPromotionOrderId(ORDER_ID))
			.thenReturn(Optional.of(promotionOrderItem));
		when(promotionProductRepository.findByIdForUpdate(PROMOTION_PRODUCT_ID))
			.thenReturn(Optional.of(promotionProduct));
		when(paymentRepository.saveAndFlush(any(Payment.class)))
			.thenThrow(new DataIntegrityViolationException(
				"Duplicate entry for key 'uk_payment_portone_payment_id'"
			));

		// when & then
		assertThatThrownBy(() -> paymentFailService.failPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.DUPLICATED_PAYMENT);

		verify(stockHistoryRepository, never()).save(any(StockHistory.class));
	}

	@Test
	@DisplayName("알 수 없는 DB 제약 오류가 발생하면 결제 실패 처리 실패 예외가 발생한다")
	void failPaymentFailWhenUnknownDataIntegrityViolationOccurs() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PromotionOrderItem promotionOrderItem = createPromotionOrderItem();
		PromotionProduct promotionProduct = createPromotionProduct();
		PaymentFailRequest request = createRequest(ORDER_AMOUNT);

		promotionProduct.decreaseStock(PURCHASE_QUANTITY);

		when(promotionOrderRepository.findByIdAndMemberIdForUpdate(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.PAID))
			.thenReturn(false);
		when(paymentRepository.existsByOrderIdAndStatus(ORDER_ID, PaymentStatus.FAILED))
			.thenReturn(false);
		when(paymentRepository.findByPortOnePaymentId(PORT_ONE_PAYMENT_ID))
			.thenReturn(Optional.empty());
		when(promotionOrderItemRepository.findByPromotionOrderId(ORDER_ID))
			.thenReturn(Optional.of(promotionOrderItem));
		when(promotionProductRepository.findByIdForUpdate(PROMOTION_PRODUCT_ID))
			.thenReturn(Optional.of(promotionProduct));
		when(paymentRepository.saveAndFlush(any(Payment.class)))
			.thenThrow(new DataIntegrityViolationException("not null constraint violation"));

		// when & then
		assertThatThrownBy(() -> paymentFailService.failPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_FAIL_FAILED);

		verify(stockHistoryRepository, never()).save(any(StockHistory.class));
	}

	/**
	 * 결제 대기 상태의 특가 주문을 생성한다.
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
	 * 결제 실패 처리에 필요한 특가 주문 상품을 생성한다.
	 */
	private PromotionOrderItem createPromotionOrderItem() {
		return PromotionOrderItem.create(
			ORDER_ID,
			PROMOTION_PRODUCT_ID,
			PRODUCT_ID,
			"테스트 특가 상품",
			PURCHASE_QUANTITY,
			UNIT_PRICE
		);
	}

	/**
	 * 결제 실패 처리에 필요한 특가 상품을 생성한다.
	 */
	private PromotionProduct createPromotionProduct() {
		PromotionProduct promotionProduct = PromotionProduct.create(
			PRODUCT_ID,
			"테스트 특가 상품",
			UNIT_PRICE,
			30,
			5,
			LocalDateTime.now().minusMinutes(10),
			LocalDateTime.now().plusMinutes(10)
		);

		ReflectionTestUtils.setField(promotionProduct, "id", PROMOTION_PRODUCT_ID);
		promotionProduct.open();

		return promotionProduct;
	}

	/**
	 * 결제 실패 요청 DTO를 생성한다.
	 */
	private PaymentFailRequest createRequest(Long amount) {
		return new PaymentFailRequest(
			ORDER_ID,
			PORT_ONE_PAYMENT_ID,
			amount,
			FAILURE_REASON
		);
	}
}
