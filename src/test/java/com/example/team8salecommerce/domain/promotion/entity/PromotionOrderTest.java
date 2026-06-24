package com.example.team8salecommerce.domain.promotion.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.team8salecommerce.global.exception.CustomException;

/**
 * 특가 주문 Entity 테스트
 *
 * 선착순 구매 이후 주문 상태 변경 흐름을 테스트한다.
 */
class PromotionOrderTest {

	@Test
	@DisplayName("특가 주문 생성에 성공한다")
	void createPromotionOrderSuccess() {
		// given
		Long memberId = 1L;
		Long promotionProductId = 10L;
		Long totalAmount = 20000L;
		LocalDateTime orderedAt = LocalDateTime.now();

		// when
		PromotionOrder promotionOrder = PromotionOrder.create(
			memberId,
			promotionProductId,
			totalAmount,
			orderedAt
		);

		// then
		assertThat(promotionOrder.getMemberId()).isEqualTo(memberId);
		assertThat(promotionOrder.getPromotionProductId()).isEqualTo(promotionProductId);
		assertThat(promotionOrder.getTotalAmount()).isEqualTo(totalAmount);
		assertThat(promotionOrder.getOrderedAt()).isEqualTo(orderedAt);
		assertThat(promotionOrder.getStatus()).isEqualTo(PromotionOrderStatus.WAITING);
	}

	@Test
	@DisplayName("대기 중인 주문은 결제 완료 상태로 변경할 수 있다")
	void markAsPaidSuccess() {
		// given
		PromotionOrder promotionOrder = createWaitingOrder();
		LocalDateTime paidAt = LocalDateTime.now();

		// when
		promotionOrder.markAsPaid(paidAt);

		// then
		assertThat(promotionOrder.getStatus()).isEqualTo(PromotionOrderStatus.PAID);
		assertThat(promotionOrder.getPaidAt()).isEqualTo(paidAt);
	}

	@Test
	@DisplayName("대기 중인 주문은 결제 실패 상태로 변경할 수 있다")
	void failPaymentSuccess() {
		// given
		PromotionOrder promotionOrder = createWaitingOrder();
		LocalDateTime failedAt = LocalDateTime.now();

		// when
		promotionOrder.failPayment(failedAt);

		// then
		assertThat(promotionOrder.getStatus()).isEqualTo(PromotionOrderStatus.PAYMENT_FAILED);
		assertThat(promotionOrder.getPaymentFailedAt()).isEqualTo(failedAt);
	}

	@Test
	@DisplayName("결제 완료된 주문은 환불 요청 상태로 변경할 수 있다")
	void requestRefundSuccess() {
		// given
		PromotionOrder promotionOrder = createWaitingOrder();
		promotionOrder.markAsPaid(LocalDateTime.now());

		LocalDateTime refundRequestedAt = LocalDateTime.now();

		// when
		promotionOrder.requestRefund(refundRequestedAt);

		// then
		assertThat(promotionOrder.getStatus()).isEqualTo(PromotionOrderStatus.REFUND_REQUEST);
		assertThat(promotionOrder.getRefundRequestedAt()).isEqualTo(refundRequestedAt);
	}

	@Test
	@DisplayName("환불 요청된 주문은 환불 완료 상태로 변경할 수 있다")
	void completeRefundSuccess() {
		// given
		PromotionOrder promotionOrder = createWaitingOrder();
		promotionOrder.markAsPaid(LocalDateTime.now());
		promotionOrder.requestRefund(LocalDateTime.now());

		LocalDateTime refundedAt = LocalDateTime.now();

		// when
		promotionOrder.completeRefund(refundedAt);

		// then
		assertThat(promotionOrder.getStatus()).isEqualTo(PromotionOrderStatus.REFUNDED);
		assertThat(promotionOrder.getRefundedAt()).isEqualTo(refundedAt);
	}

	@Test
	@DisplayName("이미 결제 완료된 주문은 다시 결제 완료 처리할 수 없다")
	void markAsPaidFailWhenAlreadyPaid() {
		// given
		PromotionOrder promotionOrder = createWaitingOrder();
		promotionOrder.markAsPaid(LocalDateTime.now());

		// when & then
		assertThatThrownBy(() -> promotionOrder.markAsPaid(LocalDateTime.now()))
			.isInstanceOf(CustomException.class);
	}

	@Test
	@DisplayName("결제 완료 상태가 아닌 주문은 환불 요청할 수 없다")
	void requestRefundFailWhenOrderIsNotPaid() {
		// given
		PromotionOrder promotionOrder = createWaitingOrder();

		// when & then
		assertThatThrownBy(() -> promotionOrder.requestRefund(LocalDateTime.now()))
			.isInstanceOf(CustomException.class);
	}

	@Test
	@DisplayName("환불 요청 상태가 아닌 주문은 환불 완료 처리할 수 없다")
	void completeRefundFailWhenOrderIsNotRefundRequested() {
		// given
		PromotionOrder promotionOrder = createWaitingOrder();
		promotionOrder.markAsPaid(LocalDateTime.now());

		// when & then
		assertThatThrownBy(() -> promotionOrder.completeRefund(LocalDateTime.now()))
			.isInstanceOf(CustomException.class);
	}

	/**
	 * 테스트용 WAITING 상태 특가 주문을 생성한다.
	 */
	private PromotionOrder createWaitingOrder() {
		return PromotionOrder.create(
			1L,
			10L,
			20000L,
			LocalDateTime.now()
		);
	}
}
