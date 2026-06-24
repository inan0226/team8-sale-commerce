package com.example.team8salecommerce.domain.stock.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.team8salecommerce.global.exception.CustomException;

/**
 * 재고 변경 이력 Entity 테스트
 *
 * StockHistory의 정적 팩토리 메서드와
 * 재고 수량 검증 로직을 테스트한다.
 */
class StockHistoryTest {

    @Test
    @DisplayName("재고 차감 이력 생성에 성공한다")
    void createDecreaseHistorySuccess() {
        // given
        Long productId = 1L;
        Long promotionProductId = 10L;
        Long orderId = 100L;
        Integer quantity = 2;
        Integer stockBefore = 10;
        Integer stockAfter = 8;

        // when
        StockHistory stockHistory = StockHistory.createDecreaseHistory(
                productId,
                promotionProductId,
                orderId,
                quantity,
                stockBefore,
                stockAfter
        );

        // then
        assertThat(stockHistory.getProductId()).isEqualTo(productId);
        assertThat(stockHistory.getPromotionProductId()).isEqualTo(promotionProductId);
        assertThat(stockHistory.getOrderId()).isEqualTo(orderId);
        assertThat(stockHistory.getPaymentId()).isNull();
        assertThat(stockHistory.getRefundId()).isNull();
        assertThat(stockHistory.getType()).isEqualTo(StockChangeType.DECREASE);
        assertThat(stockHistory.getReason()).isEqualTo(StockChangeReason.PROMOTION_PURCHASE);
        assertThat(stockHistory.getQuantity()).isEqualTo(quantity);
        assertThat(stockHistory.getStockBefore()).isEqualTo(stockBefore);
        assertThat(stockHistory.getStockAfter()).isEqualTo(stockAfter);
    }

    @Test
    @DisplayName("결제 실패 재고 복구 이력 생성에 성공한다")
    void createPaymentFailRestoreHistorySuccess() {
        // given
        Long productId = 1L;
        Long promotionProductId = 10L;
        Long orderId = 100L;
        Long paymentId = 1000L;
        Integer quantity = 2;
        Integer stockBefore = 8;
        Integer stockAfter = 10;

        // when
        StockHistory stockHistory = StockHistory.createPaymentFailRestoreHistory(
                productId,
                promotionProductId,
                orderId,
                paymentId,
                quantity,
                stockBefore,
                stockAfter
        );

        // then
        assertThat(stockHistory.getProductId()).isEqualTo(productId);
        assertThat(stockHistory.getPromotionProductId()).isEqualTo(promotionProductId);
        assertThat(stockHistory.getOrderId()).isEqualTo(orderId);
        assertThat(stockHistory.getPaymentId()).isEqualTo(paymentId);
        assertThat(stockHistory.getRefundId()).isNull();
        assertThat(stockHistory.getType()).isEqualTo(StockChangeType.RESTORE);
        assertThat(stockHistory.getReason()).isEqualTo(StockChangeReason.PAYMENT_FAILED);
        assertThat(stockHistory.getQuantity()).isEqualTo(quantity);
        assertThat(stockHistory.getStockBefore()).isEqualTo(stockBefore);
        assertThat(stockHistory.getStockAfter()).isEqualTo(stockAfter);
    }

    @Test
    @DisplayName("환불 재고 복구 이력 생성에 성공한다")
    void createRefundRestoreHistorySuccess() {
        // given
        Long productId = 1L;
        Long promotionProductId = 10L;
        Long orderId = 100L;
        Long paymentId = 1000L;
        Long refundId = 2000L;
        Integer quantity = 2;
        Integer stockBefore = 8;
        Integer stockAfter = 10;

        // when
        StockHistory stockHistory = StockHistory.createRefundRestoreHistory(
                productId,
                promotionProductId,
                orderId,
                paymentId,
                refundId,
                quantity,
                stockBefore,
                stockAfter
        );

        // then
        assertThat(stockHistory.getProductId()).isEqualTo(productId);
        assertThat(stockHistory.getPromotionProductId()).isEqualTo(promotionProductId);
        assertThat(stockHistory.getOrderId()).isEqualTo(orderId);
        assertThat(stockHistory.getPaymentId()).isEqualTo(paymentId);
        assertThat(stockHistory.getRefundId()).isEqualTo(refundId);
        assertThat(stockHistory.getType()).isEqualTo(StockChangeType.RESTORE);
        assertThat(stockHistory.getReason()).isEqualTo(StockChangeReason.REFUND_COMPLETED);
        assertThat(stockHistory.getQuantity()).isEqualTo(quantity);
        assertThat(stockHistory.getStockBefore()).isEqualTo(stockBefore);
        assertThat(stockHistory.getStockAfter()).isEqualTo(stockAfter);
    }

	@Test
	@DisplayName("결제 실패 복구 이력 생성 시 paymentId가 null이면 실패한다")
	void createPaymentFailRestoreHistory_paymentId_null이면_실패() {
		// when & then
		assertThatThrownBy(() -> StockHistory.createPaymentFailRestoreHistory(
			1L,
			10L,
			100L,
			null,
			2,
			8,
			10
		)).isInstanceOf(CustomException.class);
	}

	@Test
	@DisplayName("환불 복구 이력 생성 시 paymentId가 null이면 실패한다")
	void createRefundRestoreHistory_paymentId_null이면_실패() {
		// when & then
		assertThatThrownBy(() -> StockHistory.createRefundRestoreHistory(
			1L,
			10L,
			100L,
			null,
			2000L,
			2,
			8,
			10
		)).isInstanceOf(CustomException.class);
	}

	@Test
	@DisplayName("환불 복구 이력 생성 시 refundId가 null이면 실패한다")
	void createRefundRestoreHistory_refundId_null이면_실패() {
		// when & then
		assertThatThrownBy(() -> StockHistory.createRefundRestoreHistory(
			1L,
			10L,
			100L,
			1000L,
			null,
			2,
			8,
			10
		)).isInstanceOf(CustomException.class);
	}

    @Test
    @DisplayName("재고 변경 수량이 0 이하이면 이력 생성에 실패한다")
    void createHistoryFailWhenQuantityIsZeroOrNegative() {
        // when & then
        assertThatThrownBy(() -> StockHistory.createDecreaseHistory(
                1L,
                10L,
                100L,
                0,
                10,
                10
        )).isInstanceOf(CustomException.class);

        assertThatThrownBy(() -> StockHistory.createDecreaseHistory(
                1L,
                10L,
                100L,
                -1,
                10,
                11
        )).isInstanceOf(CustomException.class);
    }

	@Test
	@DisplayName("필수 ID 값이 null이면 이력 생성에 실패한다")
	void createHistoryFailWhenRequiredIdIsNull() {
		// when & then
		assertThatThrownBy(() -> StockHistory.createDecreaseHistory(
			null,
			10L,
			100L,
			1,
			10,
			9
		)).isInstanceOf(CustomException.class);

		assertThatThrownBy(() -> StockHistory.createDecreaseHistory(
			1L,
			null,
			100L,
			1,
			10,
			9
		)).isInstanceOf(CustomException.class);

		assertThatThrownBy(() -> StockHistory.createDecreaseHistory(
			1L,
			10L,
			null,
			1,
			10,
			9
		)).isInstanceOf(CustomException.class);
	}

    @Test
    @DisplayName("변경 전/후 재고가 음수이면 이력 생성에 실패한다")
    void createHistoryFailWhenStockBeforeOrStockAfterIsNegative() {
        // when & then
        assertThatThrownBy(() -> StockHistory.createDecreaseHistory(
                1L,
                10L,
                100L,
                1,
                -1,
                0
        )).isInstanceOf(CustomException.class);

        assertThatThrownBy(() -> StockHistory.createDecreaseHistory(
                1L,
                10L,
                100L,
                1,
                1,
                -1
        )).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("재고 차감 계산식이 맞지 않으면 이력 생성에 실패한다")
    void createDecreaseHistoryFailWhenCalculationIsInvalid() {
        // when & then
        assertThatThrownBy(() -> StockHistory.createDecreaseHistory(
                1L,
                10L,
                100L,
                2,
                10,
                9
        )).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("재고 복구 계산식이 맞지 않으면 이력 생성에 실패한다")
    void createRestoreHistoryFailWhenCalculationIsInvalid() {
        // when & then
        assertThatThrownBy(() -> StockHistory.createPaymentFailRestoreHistory(
                1L,
                10L,
                100L,
                1000L,
                2,
                8,
                9
        )).isInstanceOf(CustomException.class);
    }
}
