package com.example.team8salecommerce.domain.promotion.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.team8salecommerce.global.exception.CustomException;

/**
 * 특가 상품 Entity 테스트
 *
 * 선착순 구매에서 중요한 이벤트 시간, 상태, 재고 차감/복구 로직을 테스트한다.
 */
class PromotionProductTest {

	@Test
	@DisplayName("이벤트 진행 중이고 재고가 충분하면 구매 가능 검증에 성공한다")
	void validatePurchasableSuccess() {
		// given
		LocalDateTime now = LocalDateTime.now();
		PromotionProduct promotionProduct = createPromotionProduct(
			now.minusMinutes(10),
			now.plusMinutes(10),
			10
		);

		// 특가 상품을 판매 가능 상태로 변경한다.
		promotionProduct.open();

		// when
		promotionProduct.validatePurchasable(now, 2);

		// then
		assertThat(promotionProduct.getStatus()).isEqualTo(PromotionProductStatus.OPEN);
		assertThat(promotionProduct.getRemainingEventStock()).isEqualTo(10);
	}

	@Test
	@DisplayName("특가 상품 상태가 OPEN이 아니면 구매 가능 검증에 실패한다")
	void validatePurchasableFailWhenStatusIsNotOpen() {
		// given
		LocalDateTime now = LocalDateTime.now();
		PromotionProduct promotionProduct = createPromotionProduct(
			now.minusMinutes(10),
			now.plusMinutes(10),
			10
		);

		// open()을 호출하지 않았으므로 READY 상태다.

		// when & then
		assertThatThrownBy(() -> promotionProduct.validatePurchasable(now, 1))
			.isInstanceOf(CustomException.class);
	}

	@Test
	@DisplayName("이벤트 시간이 아니면 구매 가능 검증에 실패한다")
	void validatePurchasableFailWhenEventTimeIsInvalid() {
		// given
		LocalDateTime now = LocalDateTime.now();
		PromotionProduct promotionProduct = createPromotionProduct(
			now.plusMinutes(10),
			now.plusMinutes(30),
			10
		);

		promotionProduct.open();

		// when & then
		assertThatThrownBy(() -> promotionProduct.validatePurchasable(now, 1))
			.isInstanceOf(CustomException.class);
	}

	@Test
	@DisplayName("구매 수량이 남은 이벤트 재고보다 많으면 구매 가능 검증에 실패한다")
	void validatePurchasableFailWhenOutOfStock() {
		// given
		LocalDateTime now = LocalDateTime.now();
		PromotionProduct promotionProduct = createPromotionProduct(
			now.minusMinutes(10),
			now.plusMinutes(10),
			3
		);

		promotionProduct.open();

		// when & then
		assertThatThrownBy(() -> promotionProduct.validatePurchasable(now, 4))
			.isInstanceOf(CustomException.class);
	}

	@Test
	@DisplayName("이벤트 재고 차감에 성공한다")
	void decreaseStockSuccess() {
		// given
		LocalDateTime now = LocalDateTime.now();
		PromotionProduct promotionProduct = createPromotionProduct(
			now.minusMinutes(10),
			now.plusMinutes(10),
			10
		);

		promotionProduct.open();

		// when
		promotionProduct.decreaseStock(3);

		// then
		assertThat(promotionProduct.getRemainingEventStock()).isEqualTo(7);
		assertThat(promotionProduct.getStatus()).isEqualTo(PromotionProductStatus.OPEN);
	}

	@Test
	@DisplayName("이벤트 재고가 0이 되면 SOLD_OUT 상태로 변경된다")
	void decreaseStockToSoldOutSuccess() {
		// given
		LocalDateTime now = LocalDateTime.now();
		PromotionProduct promotionProduct = createPromotionProduct(
			now.minusMinutes(10),
			now.plusMinutes(10),
			3
		);

		promotionProduct.open();

		// when
		promotionProduct.decreaseStock(3);

		// then
		assertThat(promotionProduct.getRemainingEventStock()).isEqualTo(0);
		assertThat(promotionProduct.getStatus()).isEqualTo(PromotionProductStatus.SOLD_OUT);
	}

	@Test
	@DisplayName("이벤트 진행 중 재고 복구에 성공하면 OPEN 상태가 된다")
	void restoreStockSuccessDuringEventTime() {
		// given
		LocalDateTime now = LocalDateTime.now();
		PromotionProduct promotionProduct = createPromotionProduct(
			now.minusMinutes(10),
			now.plusMinutes(10),
			3
		);

		promotionProduct.open();
		promotionProduct.decreaseStock(3);

		// when
		promotionProduct.restoreStock(1, now);

		// then
		assertThat(promotionProduct.getRemainingEventStock()).isEqualTo(1);
		assertThat(promotionProduct.getStatus()).isEqualTo(PromotionProductStatus.OPEN);
	}

	@Test
	@DisplayName("복구 후 재고가 전체 이벤트 재고보다 많아지면 실패한다")
	void restoreStockFailWhenRestoredStockExceedsTotalStock() {
		// given
		LocalDateTime now = LocalDateTime.now();
		PromotionProduct promotionProduct = createPromotionProduct(
			now.minusMinutes(10),
			now.plusMinutes(10),
			3
		);

		promotionProduct.open();

		// when & then
		assertThatThrownBy(() -> promotionProduct.restoreStock(1, now))
			.isInstanceOf(CustomException.class);
	}

	/**
	 * 테스트용 특가 상품을 생성한다.
	 *
	 * create()는 기본적으로 전체 이벤트 재고를 남은 이벤트 재고로 설정하고,
	 * 상태는 READY로 시작한다고 가정한다.
	 */
	private PromotionProduct createPromotionProduct(
		LocalDateTime startTime,
		LocalDateTime endTime,
		Integer totalEventStock
	) {
		return PromotionProduct.create(
			1L,
			"테스트 특가 상품",
			10000L,
			20,
			totalEventStock,
			startTime,
			endTime
		);
	}
}
