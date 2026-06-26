package com.example.team8salecommerce.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.team8salecommerce.domain.payment.client.PortOnePaymentClient;
import com.example.team8salecommerce.domain.payment.client.PortOnePaymentInfo;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmRequest;
import com.example.team8salecommerce.domain.payment.dto.PaymentConfirmResponse;
import com.example.team8salecommerce.domain.promotion.entity.PromotionOrder;
import com.example.team8salecommerce.domain.promotion.repository.PromotionOrderRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;

/**
 * 결제 서비스 단위 테스트
 *
 * 외부 PortOne 검증과 트랜잭션 서비스 호출 전까지의 흐름을 검증한다.
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
	private PromotionOrderRepository promotionOrderRepository;

	@Mock
	private PortOnePaymentClient portOnePaymentClient;

	@Mock
	private PaymentConfirmTransactionService paymentConfirmTransactionService;

	@InjectMocks
	private PaymentService paymentService;

	@Test
	@DisplayName("PortOne 검증 후 결제 승인 트랜잭션 서비스를 호출한다")
	void confirmPaymentSuccess() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);
		PortOnePaymentInfo portOnePaymentInfo = createPaidPortOnePaymentInfo(ORDER_AMOUNT);
		PaymentConfirmResponse expectedResponse = createResponse();

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(portOnePaymentClient.getPayment(PORT_ONE_PAYMENT_ID))
			.thenReturn(portOnePaymentInfo);
		when(paymentConfirmTransactionService.confirmPayment(
			MEMBER_ID,
			ORDER_ID,
			portOnePaymentInfo
		)).thenReturn(expectedResponse);

		// when
		PaymentConfirmResponse response = paymentService.confirmPayment(MEMBER_ID, request);

		// then
		assertThat(response).isEqualTo(expectedResponse);

		verify(paymentConfirmTransactionService).confirmPayment(
			MEMBER_ID,
			ORDER_ID,
			portOnePaymentInfo
		);
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
		verify(portOnePaymentClient, never()).getPayment(any());
		verify(paymentConfirmTransactionService, never()).confirmPayment(anyLong(), anyLong(), any());
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

		verify(portOnePaymentClient, never()).getPayment(any());
		verify(paymentConfirmTransactionService, never()).confirmPayment(anyLong(), anyLong(), any());
	}

	@Test
	@DisplayName("이미 결제 완료된 주문이면 PortOne 조회 전에 결제 승인에 실패한다")
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

		verify(portOnePaymentClient, never()).getPayment(any());
		verify(paymentConfirmTransactionService, never()).confirmPayment(anyLong(), anyLong(), any());
	}

	@Test
	@DisplayName("결제 금액이 주문 금액과 다르면 PortOne 조회 전에 결제 승인에 실패한다")
	void confirmPaymentFailWhenAmountMismatch() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentConfirmRequest request = createRequest(9999L);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

		verify(portOnePaymentClient, never()).getPayment(any());
		verify(paymentConfirmTransactionService, never()).confirmPayment(anyLong(), anyLong(), any());
	}

	@Test
	@DisplayName("PortOne 결제 ID가 요청 결제 ID와 다르면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPortOnePaymentIdMismatch() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(portOnePaymentClient.getPayment(PORT_ONE_PAYMENT_ID))
			.thenReturn(new PortOnePaymentInfo(
				"other-payment-id",
				"PAID",
				ORDER_AMOUNT
			));

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_CONFIRM_FAILED);

		verify(paymentConfirmTransactionService, never()).confirmPayment(anyLong(), anyLong(), any());
	}

	@Test
	@DisplayName("PortOne 결제 상태가 PAID가 아니면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPortOnePaymentIsNotPaid() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(portOnePaymentClient.getPayment(PORT_ONE_PAYMENT_ID))
			.thenReturn(new PortOnePaymentInfo(
				PORT_ONE_PAYMENT_ID,
				"READY",
				ORDER_AMOUNT
			));

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_CONFIRM_FAILED);

		verify(paymentConfirmTransactionService, never()).confirmPayment(anyLong(), anyLong(), any());
	}

	@Test
	@DisplayName("PortOne 결제 금액이 주문 금액과 다르면 결제 승인에 실패한다")
	void confirmPaymentFailWhenPortOneAmountMismatch() {
		// given
		PromotionOrder promotionOrder = createWaitingPromotionOrder();
		PaymentConfirmRequest request = createRequest(ORDER_AMOUNT);

		when(promotionOrderRepository.findByIdAndMemberId(ORDER_ID, MEMBER_ID))
			.thenReturn(Optional.of(promotionOrder));
		when(portOnePaymentClient.getPayment(PORT_ONE_PAYMENT_ID))
			.thenReturn(createPaidPortOnePaymentInfo(9999L));

		// when & then
		assertThatThrownBy(() -> paymentService.confirmPayment(MEMBER_ID, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

		verify(paymentConfirmTransactionService, never()).confirmPayment(anyLong(), anyLong(), any());
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

	private PaymentConfirmRequest createRequest(Long amount) {
		return new PaymentConfirmRequest(
			ORDER_ID,
			PORT_ONE_PAYMENT_ID,
			amount
		);
	}

	private PortOnePaymentInfo createPaidPortOnePaymentInfo(Long amount) {
		return new PortOnePaymentInfo(
			PORT_ONE_PAYMENT_ID,
			"PAID",
			amount
		);
	}

	private PaymentConfirmResponse createResponse() {
		return new PaymentConfirmResponse(
			ORDER_ID,
			PAYMENT_ID,
			PORT_ONE_PAYMENT_ID,
			ORDER_AMOUNT,
			"PAID",
			"PAID",
			LocalDateTime.now()
		);
	}
}
